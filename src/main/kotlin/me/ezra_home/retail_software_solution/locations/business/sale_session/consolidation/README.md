# Consolidation Package — Rules & Expectations

This document is the canonical reference for **consolidating several sales into one new draft**: folding their lines
(`line_merge/`), the pending session that holds what cannot be folded, and selecting and generating the consolidated sale.
The session model, storage, validation split and save flow stay in the parent package's `README.md`; read it first.

Package layout: `consolidation/` holds the orchestration (`SaleConsolidationStarter`, `SaleConsolidationGenerator`,
`SaleConsolidationMerger`, `ConsolidationSources`, `ConsolidationSourcePayments`, `ConsolidationSessionCleanup`,
`ReviewedProductLines`), the pending-session pieces (`PendingConsolidationAssessor`, `PendingConsolidationPromoter`,
`PendingConsolidationRemovedLines`, `PendingConsolidationSourceStock`) and
`line_merge/` (`SaleLineMerger` and its builders). The REST-facing `SaleConsolidationHandler` and the session types
(`PendingConsolidation`, `RemovedSaleLine`, `ConsolidationIssueResponse`) stay in the parent `api/` package.

---

## 1. Line merge

`line_merge` folds the lines of several source sales into one line per product, or says why a product cannot be folded.
`SaleLineMerger.merge(saleLines, saleAdjustments, inactiveLocationProductIds, unitConversionGraph)` is a pure function
and stores nothing. It returns a `SaleLineMergeResult`: the merged lines and their adjustments, the lines and
adjustments of every product that could not be merged (untouched, so the cashier can fix them), the order-level
adjustments (untouched, because they belong to the sale, not to a product), and one `ConsolidationProblem` per blocked
product. A line-level adjustment must point at a line in the input; one that does not is refused, not dropped.

**Per product.** An inactive product is blocked. A product on a single line passes through unchanged. Lines of one product
must share a list price (`defaultSalePrice`) unless every one of them carries a typed price, because a typed price replaces
the list price in the effective total.

**Which unit the merged line uses.** The candidates are, in order: the units the lines were sold in, smallest first; the
rest of the product's unit tree (every unit under the storage unit) from the largest down; the storage unit last. The
first candidate that works wins, so a readable unit is preferred and the smallest selected one stays the first choice.
A candidate works when the combined quantity is exact with at most two decimals and under `MAX_USER_QUANTITY`, any typed
price converts to that unit exactly and fits the money scale, the adjustments combine, and the merged effective total
equals the sum of the sources' effective totals to the cent. When nothing works, the reported reason is the one from the
candidate that got furthest (`MergeStep`: product checks, quantity, adjustments, total), the smallest selected unit
winning a tie.

**Quantity.** When the target is the unit id and size of every line, quantities and gross totals are added. Otherwise the
quantity is the sum of `quantity × size` divided once by the target's size (`Decimals.exactQuotientOrNull`, which refuses a
quotient that does not terminate), and the gross total is recomputed with `SaleSessionLine.lineTotal` from the list price.
A line snapshots its unit's size when it is created and the size can be edited later, so the snapshot decides, not the unit
id. The sizes and the tree come from the `UnitConversionGraph`.

**Adjustments.** A line holds at most one price override and never one beside another adjustment
(`guardLineOverrideExclusivity`, and `PriceOverrideReconciler` swaps an override instead of adding a second). Overrides
merge when every line has one, the typed prices agree per target unit and fit the money scale, and the approvers agree; the
merged direction and amount follow `LinePricing.overrideDifference`. The other adjustments merge when every line carries the
same set (reason, direction, method, value) with the same approvers, and are then carried once. An adjustment amount is
always calculated against its own line, found by `ProductLineWithPrice.lineKey`, never by product.

**Exactness.** The final money check makes any accepted merge cost-neutral. It compares money only, so quantity exactness
rests on `MergedQuantityCalculator`.

| Kind                          | Step        | Meaning                                                                          |
|-------------------------------|-------------|----------------------------------------------------------------------------------|
| `INACTIVE_PRODUCT`            | product     | the product is not active                                                        |
| `DIFFERENT_LIST_PRICES`       | product     | list prices differ and not every line carries a typed price                      |
| `UNITS_NOT_EXACT`             | quantity    | the combined quantity is not a terminating number in that unit                   |
| `QUANTITY_TOO_PRECISE`        | quantity    | the combined quantity needs more than two decimals                               |
| `QUANTITY_TOO_LARGE`          | quantity    | the combined quantity is over the maximum                                        |
| `OVERRIDE_ON_SOME_LINES_ONLY` | adjustments | some lines carry a typed price and others do not                                 |
| `TYPED_PRICES_DIFFER`         | adjustments | the typed prices differ per target unit                                          |
| `TYPED_PRICE_NOT_TYPEABLE`    | adjustments | the typed price per target unit is not exact or has more than two decimals       |
| `APPROVERS_DIFFER`            | adjustments | the same adjustment was approved by different people                             |
| `OTHER_ADJUSTMENTS_DIFFER`    | adjustments | the lines carry different percentage adjustments                                 |
| `TOTAL_DRIFT`                 | total       | the merged effective total would differ from the sources' sum                    |

**Story.** `ConsolidationProblemStoryWriter.write(consolidationProblems, problemSaleLines, problemSaleAdjustments,
sourceSaleReferenceByLineKey, unitNameByUnitId)` turns each `ConsolidationProblem` into a `BlockingProblem`: `documentReference` is the source sale
references involved (each once, in line order, joined by ", "), `description` says what is wrong with the product, and
`effect` lists the lines (quantity with the unit pluralized, the price per unit, source sale) and says how to fix it. The
price shown is the one on the form: the cashier's price when the line has an override, otherwise the list price, with no
label, because a typed price equal to the list price is saved as no override and a label would contradict the form. Only
`OVERRIDE_ON_SOME_LINES_ONLY` labels lines "(changed by the cashier)" or "(normal price)". Texts never say "typed price". It switches exhaustively on
`ConsolidationProblemKind`, so a new kind will not compile until it has text. The caller supplies the sale reference per
line key and the unit names; the writer fetches nothing.

## 2. Pending consolidation session

A `SaleSession` has a `kind`: `SALE` (every ordinary session) or `PENDING_CONSOLIDATION`. The kind and
`pendingConsolidation` (the source sales with their ids, versions and references, the source sale reference per line key,
the removed lines and the stock the sources hold) are present together, and a pending consolidation never has a sale id,
a sale version or payments; the constructor refuses any other combination. `SaleSessionBuilder.asPendingConsolidation` is
the only way to make one. The session lives in Redis only and holds just the lines of the products that could not be merged.
No sale exists for it until a consolidation is generated, which is why it has no `saleId`.

**One rules table.** `SaleSessionRules` (api) answers, per kind, which `SaleSessionAction` is allowed and why not:
`refusalOrNull(kind, action)` is an exhaustive `when` per kind, so adding an action or a kind does not compile (and
`SaleSessionRulesTest` fails) until every pairing is decided. Every mutating handler starts with
`SaleSessionValidator.guardAllowed(session, action)`, which applies the table and then, for every action except payments,
void and notes, the status rule (`SaleSessionAction.needsMutableSale()`: the sale must still be a DRAFT). The header handler maps each
provided field to its own action (`UPDATE_CONTACT`, `UPDATE_SOLD_BY`, `UPDATE_DATE_SOLD`, `UPDATE_NOTES`); an empty update is
held to the notes rule, which every sale allows. Notes are open on a confirmed, voided or discarded sale too: the header
handler writes them to the sale at once through `SaleUpdater.updateNotes` (a cart with no sale yet keeps them in the session
until it is saved) and takes the new sale version into the session. `validate` reads the same table for content: `allowsDuplicateProducts` and the
`ADD_ORDER_LEVEL_ADJUSTMENT` answer. A pending consolidation allows notes, line edits and removals, restoring a removed
line, line-level adjustments, removing adjustments and discarding (void). It refuses adding a line (`ADD_LINE`: the
cashier fixes or removes what is shown and adds products to the generated draft), contact, sold-by, date-sold, order-level
adjustments, payments, save-draft and confirm. Nothing detects a handler that does not call `guardAllowed`; a new handler
must.

**Issues.** `PendingConsolidationAssessor.assess(session, inactiveLocationProductIds, unitConversionGraph)` runs
`SaleLineMerger.merge` on the held lines and passes the problems to `ConsolidationProblemStoryWriter`. The assembler loads
the inactive product ids and the unit graph once per response and hands them to the assessor and to the line mapping.
The pending session also records `cleanLocationProductIds` (the products that combined cleanly at selection and are not held
on the form). Generate merges those again, so `PendingConsolidationPromoter` runs when the form is reloaded
(`SaleSessionHandler.acquireSession`) and when Generate is pressed: a clean product that has turned inactive is copied from the
sources onto the form as held lines and leaves the clean set. It is then an ordinary held product, flagged by the merger like
any other, listed with its lines and removable. Generate that finds such a product saves the updated session and refuses with
"Some products still cannot be combined. Reload this page to see them".

The response carries `kind`, `consolidationIssues` (`saleReferences`, `description`, `fix`, `blocking`) and
`uiOptions.canGenerateConsolidatedSale`, true when no blocking issue remains. A non-blocking entry is a warning on the same
list: the sources that have a sale-wide adjustment (`PendingConsolidation.saleReferencesWithOrderLevelAdjustments`) are named,
because a pending consolidation does not carry it onto the new sale. The flag only drives the button; Generate re-runs the
check on the server. Every held line has a source sale, since lines cannot be added.

**Removed lines.** `PendingConsolidationRemovedLines.remove` moves a line, with its line-level adjustments, into
`pendingConsolidation.removedSaleLines` instead of deleting it, and `restore` puts it back in its old position (it keeps
its `rank`). `SaleSessionRules.keepsRemovedLines` chooses it for the pending kind; a sale just uses
`SaleSession.withoutLine`, and `restore` is refused for it. The response lists them as `removedSaleLines` (product,
quantity, unit, price, line total, source sale) for a "Removed lines" tab, and
`POST /secured/sale-sessions/{sessionId}/lines/restore` (`RESTORE_REMOVED_LINE`) restores one. Removed lines are not
assessed and are left out of the generated sale. Only held lines (problem products) can be removed, so a clean product
cannot be dropped from a pending session.

**Line order.** Each `SaleSessionLine` has a `rank` (decimal, also on `SaleSessionLineResponse`), and the session keeps its
lines sorted by it (`SaleSessionUpdateFinalizer`). `SaleLineRanker` assigns it: a persisted sale and a single-sale copy
are ranked alphabetically by product (case-insensitive: 1, 2, 3 ...); a consolidation gives each product one whole rank,
in alphabetical order, and each further line of the same product a hundredth more (2.00, 2.01), so like products stay
together; a line added in the session takes ranks below the lowest existing one (the first added is on top). Ranks are
not stored on the sale: reloading a saved sale ranks alphabetically again. A merged line keeps the rank of its first
source line.

**Stock figures.** A pending session has no sale id, so the overlay cannot discount a reservation of its own. Instead
`PendingConsolidation.stockHeldBySourceSalesByLocationProductId` holds, per product, what the source sales hold
(`StockHeldBySales`: reserved by DRAFT sources, taken by CONFIRMED ones, nothing for a DISCARDED or VOIDED one, which cannot be selected), calculated in
storage units by `PendingConsolidationSourceStock.calculate` when the consolidation is selected. The overlay
(`SaleSessionStockOverlay`) shows the stock as if those sales were already gone: on-hand plus the taken amount, reserved
minus the reserved amount, available plus both. A saved draft gets the same treatment for its own reservation, which is
the only case where the overlay looks reservations up. The snapshot is not refreshed, because a source that changes
makes Generate abort anyway. The figures cover held products only, so a product that merges cleanly is not shown and can
still turn out short when the draft is saved, which fails the save as for any draft.

**What the form is told.** `SaleSessionUiOptionsBuilder` builds `uiOptions` from `SaleSession.allows(action)` (the rules
table plus the DRAFT check): `canAddLines`, `canEditLines`, `canChangeContact`, `canSaveDraft`, `canConfirm`,
`canAddPaymentsToSale`, `canCopySale` and `allowedDiscardAction` (`DISCARD_DRAFT`; `VOID_SALE` for a confirmed sale once no
payment is left on it; `DISCARD_PENDING_CONSOLIDATION`; or null). `saveDraftBlockers` and `confirmBlockers` say why an
allowed save or confirm is not ready: no lines, an unpaid walk-in, payments above the total, an inactive product. They use
the confirm guards' own predicates (`SaleSession.walkInIsNotFullyCovered`, `paymentsExceedTotal`) and
`SaleSessionUiOptionsBuilderTest` checks the line, walk-in and payment blockers against their guards; the inactive-product
blocker comes from the same lookup the response already makes. They describe the session as last saved, so
edits not yet sent are the form's own check. The form keeps no copy of these rules and does not sort the lines: the
response is already in rank order.

**Listing and warnings.** `getSessionsForUnsavedSales` lists a pending consolidation beside new sales, since it has no
`saleId`, and `SaleSessionSummaryDto.kind` lets the UI label it (its `receivableTotal` is only the held lines' total). It
never reserves stock (`SaleSessionRules.reservesStock`), so `showUnreservedChangesWarning` is always false for it.

**Starting a session.** `SaleSessionLoader` and `SaleSessionCopier` return a new session with its stock figures filled
in and its totals not yet computed; `SaleSessionUpdateFinalizer.finalize` computes them, so a caller must finalize what
it starts. Selecting sales and generating the draft are described in section 3.

## 3. Selecting sales and generating the consolidated draft

Two endpoints, both thin delegators to `SaleConsolidationHandler` (one location-schema transaction each):
`POST /secured/sale-sessions/consolidate` with `{ saleIds }`, and
`POST /secured/sale-sessions/{sessionId}/generate-consolidated-sale` (`GENERATE_CONSOLIDATED_SALE`, allowed only for a
pending consolidation).

**Which sales qualify.** Only DRAFT and CONFIRMED sales (`SaleSessionRules.isConsolidatable`). The rule differs from Copy
Sale (`isCopyable`, which refuses CONFIRMED) on purpose: a copy is only a Redis session, so nothing makes a cashier void the
original afterwards and a confirmed original would leave two live sales; a consolidation voids its sources itself, with their
payments, so that risk does not exist. DISCARDED and VOIDED sales are refused because consolidation would have nothing to void
and they carry no live work.

**Selection** (`SaleConsolidationStarter`). Takes the SALE advisory locks of the selected sales first, in id order
(`ConsolidationSources.lock`), then reads their headers, because a header read before the locks would come from the
persistence context and could not show a change made in between. It then guards the selection: 2 to 20 distinct sales, one
contact, none walk-in, all qualifying. It copies the sales raw (`SaleSessionCopier.copyFromSalesWithSourceReferences`, which
also names the source sale of every line and returns the lines it loaded) and runs `SaleConsolidationMerger`. With no problem
it hands the copy and the merge to `SaleConsolidationGenerator.generateFromMerged`, so nothing is locked, copied or merged a
second time, and the response is the saved new draft. With problems it opens a pending session holding only the problem
products' lines (and their line-level adjustments), with the source ids, the versions read at selection and the stock snapshot
(built from the lines the copy already loaded), in Redis only. No source is touched.

**Generate** (`SaleConsolidationGenerator`, runs in the caller's transaction). For a pending session `generate` locks every
source (`ConsolidationSources.lock`), refuses if any version differs from the one recorded at selection, re-runs the selection
guards, copies the sources again and merges the clean products' lines recomputed from the sources together with the lines the
cashier reviewed (`ReviewedProductLines`: the lines left in the pending session, fixed or not, and the products whose lines
were all removed, which are dropped rather than recomputed). Both paths then run `generateFromMerged`: refuse if a problem
remains; combine the order-level adjustments (`OrderLevelAdjustmentsCombiner`); validate; allocate the reference
(`SaleReferenceAllocator`); void the active payments of the sources with the reason "Consolidated into <ref>"
(`ConsolidationSourcePayments`); void the sources together (`SaleUpdater.voidSourcesOfConsolidation`); save the draft with
`preAllocatedReferenceNumber` and the payments re-recorded with a new identity and no payment date, so each is dated when it is
saved, not at the source payment's date (the void of the source payment is dated the same day); load the saved draft as the
new session. A stock shortage fails the save, which rolls the whole transaction back, so the sources and the pending session
are as they were.

**After the commit only.** `ConsolidationSessionCleanup` deletes the open sessions of the source sales and the pending session,
then saves the new session to Redis, in a `TransactionSynchronization.afterCommit`, so a rolled-back Generate never leaves
Redis pointing at a sale that does not exist. A delete that fails is logged only, since the session expires by itself. A save
that fails is rethrown as one sentence for the error summary: the sales were consolidated, and the cashier refreshes and opens
the new draft from the sales grid, where it is rebuilt from the database.

**Order-level adjustments.** Adjustments of the same reason, direction, method and approver become one. An order-level
percentage is money on its own sale (`ConsolidationCopy.orderLevelAmountByAdjustmentKey`, the percentage of that sale's
goods total), so the sources' amounts are added as money and turned back into one percentage of the combined goods total,
only when that percentage is exact (at most 4 decimal places, and it reproduces the same amount). Two 5% discounts on
equal sales stay 5%; a 5% discount on one of two equal sales becomes 2.5%. When it is not exact, Generate is refused and
nothing is voided. One with a different approver stays separate, so an approval is never attributed to an amount its
approver did not see. Notes are joined with "; " without repeats.

**Sale-wide adjustments in a pending consolidation.** A percentage of the whole sale describes the goods of its own sale, so it
is carried only when the sales are consolidated at once: with no line problem, and with a combined percentage that is exact,
Select carries them as above. When a pending consolidation opens, for a line problem or because the percentage is not
exact, Generate leaves every sale-wide adjustment off the new sale, and the Issues list says so with a non-blocking warning
that names the sources. The form cannot edit sale-wide adjustments, so the cashier adds the discount again on the new sale.
Nothing is removed or recomputed on the cashier's behalf, whatever lines they remove.
