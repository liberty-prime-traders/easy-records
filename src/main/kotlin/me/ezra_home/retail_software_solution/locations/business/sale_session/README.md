# Sale Session Package — Rules & Expectations

This document is the canonical reference for **how a sale session behaves** in
RTSS. A sale session is the **only** path to creating or confirming a sale.

If you are about to change behavior inside this package, scan this file first
and update it when the rule changes.

---

## 1. Scope & Public Surface

The package lives at:

```
locations/business/sale_session/
locations/business/sale_session/api/
```

Public surface (everything else in this package is internal):

| Class                          | Purpose                                                                      |
|--------------------------------|------------------------------------------------------------------------------|
| `SaleSessionHandler`           | Start, abandon, list, get a session                                          |
| `SaleSessionHeaderHandler`     | Patch contactId, soldById, dateSold, notes                                   |
| `SaleSessionLineHandler`       | Add, update, remove session lines                                            |
| `SaleSessionAdjustmentHandler` | Add, remove session adjustments                                              |
| `SaleSessionPaymentHandler`    | Add, remove session payments                                                 |
| `SaleSessionPersister`         | `saveDraft`, `confirm`, `voidSale` — server-side commit / terminal pipelines |

Internal collaborators (do not call from outside `sale_session`):

- `SaleSessionStore` — storage interface; `SaleSessionRedisStore`
  (`redis-active` profile) or `SaleSessionInMemoryStore` (default —
  `!redis-active`)
- `SaleSessionBuilder` — stamps session id, location, user and timestamps from the request context; the loader
  composes the rest (sale link, header, lines, adjustments, payments, reservation state) onto it
- `SaleSessionLoader` — `loadFromSale` rehydrates an existing `SaleEntity` into
  the session shape via `DomainToSessionMapper`; `newSession` mints a fresh
  empty session
- `SaleSessionCopier` — `copyFromSale` mints a new unsaved session from one source sale;
  `copyFromSalesWithSourceReferences` copies the lines and adjustments of 2 to 20 distinct sales of one contact
  (`guardConsolidationSelectionSize`, `guardSingleContactAcrossSales`) under fresh identities, each adjustment pointed at
  the copy of its own sale's line, with `<reference> - <notes>` lines for sources that have notes. The copy keeps every
  line as it was, so a product can appear on several lines; `line_merge` (section 11) is what folds them together
- `SaleSessionAssembler` — orchestrates response/summary DTO assembly;
  delegates per-bean translation to `SessionToResponseMapper` (MapStruct)
- `SaleSessionValidator` — runs structural validation on every mutation
- `SaleSessionTotalsCalculator` — recomputes totals after every mutation
- `SaleSessionUpdateFinalizer` — touch → recompute → validate → save → buildResponse
  pipeline invoked by every mutation handler

Architectural boundary:

```
sale_session/ → may call sale/api/, sale_adjustment/api/, sale_payment/api/, stock/api/
sale/         → knows nothing about sale_session (convention)
```

`sale_session → stock/api/` is used by `SaleSessionStockOverlay` to populate the
three per-line storage quantities (`storageQuantityOnHand`, `storageQuantityReserved`,
`storageQuantityAvailable`), held exact and unrounded; the response derives its `quantityOnHand` / `quantityReserved` /
`quantityAvailable` (product base unit) from them. It calls `StockAvailabilityFetcher.fetch` (shared with
`LocationProductForSaleAssembler` and `stock_transfer`'s
`ReconciledTransferLineFetcher.draftLines`) for the sale-agnostic baseline —
on-hand balance netted against the **total** of live reservations, no
exclusion — then separately calls `StockReserver.loadReservationBreakdown`
itself to read this session's own reservation via
`ProductReservations.forSale(session.saleId)` and adds it back onto
`quantityReserved`/`quantityAvailable`, so editing a line in a DRAFT session
does not count its own prior reservation against itself. This is a second,
accepted duplicate `loadReservationBreakdown` call (one inside the shared
fetcher, one local to the overlay) — the same tradeoff `stock_transfer`'s
README documents for `guardSufficientStock`/`consumeStockForDispatch`. The
overlay is advisory only — it never throws. Session mutations and loads run
it so the UI can warn the cashier when a requested quantity exceeds
availability; the hard reject happens at `saveDraft` / `confirm` via
`SaleValidator.guardSufficientStockForSale`.

`SaleSessionLineHandler.removeLine` is a partial exception to the buffered
model: when a removed line is persisted (`identity.id != null`), the handler
eagerly calls `StockReserver.clearBySaleLineIds(...)` so the stock hold is
released immediately rather than waiting for `saveDraft`. This prevents
phantom reservations from blocking other customers while the cashier hasn't
yet committed the edit. The `SaleLineEntity` itself stays buffered — it
gets dropped at `saveDraft` via `SaleLineSync`. Consequence: abandoning a
session after `removeLine` leaves the line in DB but with no reservation;
re-opening the sale will show the line again and `saveDraft` will re-reserve
fresh (or fail with insufficient stock if another sale claimed it in the
interim).

`ArchitectureTest` enforces the `.api` boundary symmetrically — every domain's
non-`api` classes are package-private to that domain. The "sale never imports
sale_session" direction is a convention upheld by review, not by the test.

---

## 2. Lifecycle

```
[New Sale btn] ──► SaleSessionHandler.start({contactId})
[Edit Sale btn] ──► SaleSessionHandler.start({contactId, saleId})
[Copy Sale btn] ──► SaleSessionHandler.start({copyFromSaleId})
                          │
                          ▼
        ┌────────── session in Redis (TTL 2h) ──────────┐
        │  Mutations stay in Redis. Each mutation:       │
        │   1. load session                              │
        │   2. apply edit                                │
        │   3. recompute totals                          │
        │   4. validate structurally                     │
        │   5. save session                              │
        └─────────────────────┬──────────────────────────┘
                              │
      ┌────────────────────┬────────────────────┬────────────────────┐
      │                    │                    │                    │
      ▼                    ▼                    ▼                    ▼
POST {id}/draft     POST {id}/confirm    POST {id}/payments     POST {id}/void     DELETE {id}
DraftSalePersister  ConfirmedSalePersister (CONFIRMED branch:    SaleUpdater.       (abandon)
(persists at        (persists at          SalePaymentService    voidSale           drops session,
 DRAFT status;       CONFIRMED status;    .recordPayment        (DRAFT→DISCARDED  no DB writes
 session lives on)   then deletes         per add call)         or CONFIRMED→
                     session)                                   VOIDED; then
                                                                deletes session)
```

`saveDraft` flushes pending state to the DB and keeps the session alive for
further edits; only `confirm` and `abandon` actually terminate the session.

`SaleSessionHandler.start` short-circuits when an active session already exists
for the supplied `saleId` — returns the existing one (one-session-per-sale
guarantee).

`start({copyFromSaleId})` mints a new unsaved DRAFT session from a DRAFT, VOIDED or DISCARDED sale
(`SaleSessionCopier.copyFromSale`); a CONFIRMED source is rejected by `guardSourceSaleCopyable`, because copying
a sale that really happened makes an accidental duplicate easy. It never short-circuits, and may be called
repeatedly for the same source. It reads the persisted sale, so unsaved edits in the source's open session are not copied. The copy
has `saleId = null`, fresh transient identities on every line and adjustment (adjustments re-keyed to the new
line identities), no payments, `contactId` and `notes` carried, and `referenceNumber`, `soldById` and
`dateSold` blank. Lines keep the stored `defaultSalePrice`, `lineTotal` and
`storageUnitsPerUserProvidedUnit`; adjustment amounts are recomputed by the normal totals pass. `productsVersion`
starts at 1 so the unreserved-changes warning shows. No void info carries over. The response's `id` is the session
id; `saleId` is the persisted sale's id (null until the first save) and is what the UI sends as `copyFromSaleId`.

Loading is **status-agnostic** — sales in any state (DRAFT, CONFIRMED, VOIDED,
DISCARDED) can be loaded into a session for display. The session records the
sale's status at load time as `originalStatus`, which gates what mutations the
handlers allow:

| `originalStatus` | Lines / adjustments / header | Add payment                      | Remove payment                                                              | Save path              |
|------------------|------------------------------|----------------------------------|-----------------------------------------------------------------------------|------------------------|
| `DRAFT`          | ✅ allowed                    | ✅ allowed (buffered)             | ✅ transient: dropped; persisted: voided (row stays, marked)                 | `saveDraft`, `confirm` |
| `CONFIRMED`      | ❌ rejected                   | ✅ allowed (recorded immediately) | ✅ persisted only: voided (row stays, marked)                                | none (no commit step)  |
| `VOIDED`         | ❌ rejected                   | ❌ rejected                       | transient: n/a (unreachable); persisted: rejected (`guardSaleNotVoided`)    | none (view-only)       |
| `DISCARDED`      | ❌ rejected                   | ❌ rejected                       | transient: n/a (unreachable); persisted: ⚠️ leaks through (see prose below) | none (view-only)       |

`SaleSessionValidator.guardAllowed` (the kind table, then the DRAFT-only status rule) enforces the
line/adjustment/header rules and `SaleSessionValidator.canAddPayments` gates add-payment. The commit
handler calls `guardAllowed` before either commit path. There is no status
guard on the transient-discard branch: transient payments only exist on
DRAFT sessions by construction (CONFIRMED-session adds short-circuit to
`persisted` identities, and VOIDED/DISCARDED sessions reject `add` outright),
so the transient drop branch is only reachable on DRAFT.

The single `DELETE /payments` endpoint handles both removal cases: when the
payment's `SessionIdentity` is `transient` the handler drops it from the
session in-place; when it's `persisted` the handler forwards to
`SalePaymentService.voidPayments` (a `voidReason` is required in that case),
which in turn calls `SalePaymentValidator.guardSaleNotVoided`. That keeps the
frontend wire-protocol uniform — server decides which side to take based on
the payment's identity. Note `guardSaleNotVoided` only checks the VOIDED
status; DISCARDED is not blocked there, so removing a persisted payment from
a DISCARDED-loaded session is not currently rejected on the service side.

For new sales (no `saleId`), `originalStatus` is seeded to `DRAFT`.

CONFIRMED-session payments **short-circuit**: `SaleSessionPaymentHandler.add`
calls `SalePaymentService.recordPayment` immediately, then folds the persisted
payment back into the session so the cashier's view stays in sync. There is no
separate commit step — every CONFIRMED-session payment exists in the DB the
moment the handler returns. DRAFT sessions still buffer payments (the sale may
not yet exist) and flush them at `saveDraft` / `confirm`. Removing a persisted
payment from a CONFIRMED session routes through `SalePaymentService.voidPayments`
with a `voidReason`.

---

## 3. SessionIdentity

Every row in a session (line, adjustment, payment) carries a
`SessionIdentity` exposed on the row as `identity` (not `id` — the field is
deliberately named after its type so callers don't confuse it with a plain
UUID). Adjustments also carry a `saleSessionLineIdentity: SessionIdentity?`
pointing at the sale session line they're attached to (null = order-level).

```
SessionIdentity
  ├── id: UUID?          — set when the row exists in DB
  └── transientId: UUID? — set when the row only exists in Redis
```

- Exactly one is non-null — enforced by the constructor.
- Frontend echoes it back as-is when targeting a row.
- On commit:
  - `id != null`:
    - **lines** are updated in place — `SaleLineSync` writes the
      current session values for `locationProductId`, `userProvidedQuantity`, `unitId`,
      `lineTotal`, `storageUnitsPerUserProvidedUnit` onto the existing entity.
      `locationProductId` never changes (no session handler mutates it);
      `lineTotal` is recalculated by `SaleSessionLineHandler.applyLineChanges`
      whenever an add or update entry touches the line, and only then (see Pitfalls §8).
    - **adjustments** and **payments** are kept untouched — there are no
      "update" handlers for these in-session, so existing rows can only be
      retained or removed/voided, never mutated
  - `transientId != null` → insert a new row, flip `transientId` to `id` after.
    For adjustments, `applySaleSaveResult` also flips `relatedSaleLineIdentity`
    from transient to persisted when the referenced line was newly inserted in
    the same commit, keeping adjustment→line references consistent after save.
  - Adjustments missing from the input are deleted by `SaleAdjustmentSyncer`.
    Payments work differently: `SalePaymentAppender.appendNew` only ever
    inserts new (`existingId == null`) payments; persisted payments are
    neither updated nor deleted at commit. The supported way to reverse a
    posted payment is `DELETE /payments` with a `voidReason`, which goes
    through `SalePaymentService.voidPayments` and marks the row voided
    in-place — the session keeps the payment with `voidedReason` set rather
    than removing it from its list.

`SessionIdentity.key()` returns whichever UUID is set; use it as a lookup key
inside the session.

---

## 4. Storage

`sessionId` is a `UUID` end-to-end — generated by `SaleSessionHandler.start`,
echoed by every endpoint, and stored on `SaleSession.sessionId`. The Redis
keys below string-encode the UUID at the boundary (`.toString()`) and decode
on read (`UUID.fromString`).

Up to two Redis keys per session plus a global index:

```
sale-session:{sessionId}      → JSON-serialized SaleSession (TTL 7200s, hard-coded)
sale-session:by-sale:{saleId} → sessionId text (set only when session.saleId != null)
sale-session:open             → SET of sessionId text driving `listOpenSessions()`
```

`SaleSessionRedisStore` keeps all three in lockstep on every save / delete.
`SaleSessionInMemoryStore` is API-equivalent for tests / local dev.

`lastAccessedById` + `lastAccessedAt` update on every mutation
(`SaleSessionUpdateFinalizer.finalize` → `markTouched`), on
`SaleSessionHandler.acquireSession` (→ `markVisited`), and on successful
`saveDraft` (the persister bumps both inside `applySaleSaveResult` before saving).
`start` short-circuits and `listOpenSessions` are passive — they don't bump
the timestamp. This is what drives `SaleSessionSummaryDto.activeUser`.

---

## 5. Validation Split

| When           | Who                      | What                                                                                                                                                                                                                                                                                                                |
|----------------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Every mutation | `SaleSessionValidator`   | Positive quantities, no duplicate products, valid adjustment line refs, adjustment reason + direction valid, positive payments, every line total, adjustment amount, the subtotal and the sale total within `Decimals.MAXIMUM_TRANSACTION_AMOUNT`, line + order discount ceilings                                                                                                                                                      |
| Save Draft     | `DraftSalePersister`     | If any payments in the input, future-date guard + fiscal period open; stock availability for requested quantities (`SaleValidator.guardSufficientStockForSale`); optimistic `@Version` check on `SaleEntity` (existing sales only). Note: drafts do NOT enforce a payments-within-payable-total ceiling.              |
| Confirm        | `ConfirmedSalePersister` | At least one line (enforced upstream by `SaleSessionValidator.guardNonEmptyLines`); future-date guard; fiscal period open; walk-in full coverage (enforced upstream); payments within payable total (enforced upstream); stock availability for requested quantities (`SaleValidator.guardSufficientStockForSale`); FIFO stock consumption; optimistic `@Version` check on `SaleEntity` (existing sales only). |

Structural validation runs on **every** mutation; commit-time guards run only
when the session is committed. The commit-time guards exist because the world
can change while the session sits in Redis (stock moves, fiscal period closes,
product gets deactivated).

---

## 6. Save Flow

### Save Draft (`POST /secured/sale-sessions/{sessionId}/draft`)
```
1. SaleSessionPersister.saveDraft
2. `guardAllowed` (`SAVE_DRAFT` / `CONFIRM`), recompute totals, run full structural `validate`
   (`loadAndValidate`, which also runs `guardNoInactiveProducts`)
3. transform SaleSession → SaleSaveRequest (`SessionToSaveRequestMapper`)
4. DraftSalePersister.saveDraft:
     - if payments present: guard date-not-future + fiscal period open
       (`guardFiscalIfPayments`)
     - load-or-create SaleEntity at DRAFT (`loadDraftAtVersion` for existing —
       rejects non-DRAFT or version mismatch; new sales skip the version
       check and start at DRAFT)
     - sync lines (`SaleLineSync.sync` — insert new, update existing,
       delete missing)
     - clear reservations for this sale; if any persisted lines remain,
       guard stock availability (`guardSufficientStockForSale`), then re-reserve
     - SaleSaveFinalizer.finalize:
         - sync adjustments (`SaleAdjustmentSyncer.sync` — delete missing,
           update kept, insert new)
         - apply line + adjustment totals onto the sale (`SaleTotalsApplier`)
         - append new payments only (`SalePaymentAppender.appendNew`)
         - set `paymentStatus`, save sale
5. apply outcome to session (`SaleSessionPersister.applySaleSaveResult`):
   saleId, version, sync header (`referenceNumber`, `dateSold`, `soldById`
   from the persisted entity), flip transient→id where applicable, copy the
   resolved `paymentDate` onto each new session payment, bump
   `lastUpdatedAt` / `lastAccessedAt` / `lastAccessedById`
6. recompute totals, save session back
```

### Confirm (`POST /secured/sale-sessions/{sessionId}/confirm`)
```
1. SaleSessionPersister.confirm
2. `guardAllowed` (`SAVE_DRAFT` / `CONFIRM`), recompute totals, run full structural `validate`
   (`loadAndValidate`, which also runs `guardNoInactiveProducts`)
3. `guardNonEmptyLines`, `guardWalkInFullyCovered`, `guardPaymentsWithinTotal`
4. transform SaleSession → SaleSaveRequest (`SessionToSaveRequestMapper`)
5. ConfirmedSalePersister.confirm:
     - guard date-not-future + fiscal period open (always, not gated on
       payments-present like the draft path)
     - load-or-create SaleEntity (`loadDraftAtVersion` for existing —
       rejects non-DRAFT or version mismatch; new sales start at CONFIRMED)
     - assign header, save sale
     - clear reservations for this sale (`clearBySales`) — before sync, same
       FK-ordering reason as the draft path
     - sync lines (`SaleLineSync.sync`)
     - guard stock availability (`SaleValidator.guardSufficientStockForSale`
       — this sale's own reservations are already cleared, so it checks
       against every other sale's live reservations)
     - flip status to CONFIRMED
     - SaleSaveFinalizer.finalize:
         - sync adjustments (`SaleAdjustmentSyncer.sync`)
         - apply totals (`SaleTotalsApplier`)
         - append new payments (`SalePaymentAppender.appendNew`)
         - set `paymentStatus`, save sale
     - run FIFO consumption via `SaleStockUpdater.consumeStock` (acquires the
       PRODUCT advisory lock again immediately before consuming, as a
       physical-stock backstop)
     - publish `SaleConfirmedEvent` via `SaleConfirmedHandlerForKafka`
6. apply outcome to session (`applySaleSaveResult` — new ids, header
   sync from persisted entity (`referenceNumber`, `dateSold`, `soldById`),
   `paymentDate` copied onto each new session payment, `lastUpdated*` /
   `lastAccessed*` bumped), build response, delete the session
```

### Void (`POST /secured/sale-sessions/{sessionId}/void`)
```
1. SaleSessionPersister.voidSale(sessionId, SaleSessionVoidDto(reason))
2. load session; reject if session has no persisted saleId
3. delegate to SaleUpdater.voidSale(SaleVoidCreateDto(saleId, reason)):
     - DRAFT  → DISCARDED (clears reservations)
     - CONFIRMED → VOIDED (restores stock, writes SaleVoidEntity,
       publishes SaleVoidedEvent; requires today's fiscal period open)
4. reflect resulting status on the session, build response, delete session
```

Any response built for a `VOIDED` session — whether right after this flow or
on a later view of a persisted session — carries `saleVoidInfo` (voided-by,
voided-on, voided-reason), sourced from the sale's `SaleVoidEntity` via
`SaleDataFetcher.getSaleVoidInfo`. A `VOIDED` sale is expected to always have
exactly one `SaleVoidEntity`, written in the same flow that flips its status;
`SaleSessionAssembler` treats a missing one as a data-integrity violation and
throws rather than silently omitting the field.

### CONFIRMED-session payments (no commit step)
When `originalStatus == CONFIRMED`, `POST /secured/sale-sessions/{sessionId}/payments`
forwards the payment to `SalePaymentService.recordPayment` synchronously and
folds the persisted id back into the session. There is no batched commit
endpoint — each add is durable when the handler returns.

The persisters live in `sale/api/` but operate on a `SaleSaveRequest` DTO —
they do not know about `SaleSession`. The translation `SaleSession →
SaleSaveRequest` happens through `SessionToSaveRequestMapper`, called from
`SaleSessionPersister`. This keeps the package boundary intact.

---

## 7. Optimistic locking

`SaleEntity.version` is a `@Version`-managed `Long`. When a session was loaded
from an existing draft, the session captures the version at load time
(`SaleSession.saleVersion`). On commit, `SaleDataFetcher.loadDraftAtVersion`
reloads the entity, asserts status is still DRAFT, and throws
`ObjectOptimisticLockingFailureException` upfront when the captured version
doesn't match — fail-fast at load time, rather than waiting for Hibernate's
flush-time conflict.

Mid-session operations that hit the sale row out-of-band must refresh
`SaleSession.saleVersion` so the next commit doesn't trip the check. Today
that means `SaleSessionPaymentHandler.add` (when `originalStatus == CONFIRMED`
and `SalePaymentService.recordPayment` runs) and `SaleSessionPaymentHandler.remove`
(when the payment was already persisted and routes through
`SalePaymentService.voidPayments`). Both bump `SaleEntity.version` via
`SaleUpdater.updatePaymentStatus`; the new version rides back on
`SalePaymentResponseDto.updatedSaleVersion` and the handlers copy it onto the
session before finalize.

---

## 8. Pitfalls

- **Persisted payments are not mutated at commit time.**
  `SalePaymentAppender.appendNew` only inserts payments with `existingId == null`;
  it does not update, delete, or validate the persisted payments already on
  the sale. The supported way to reverse a posted payment is `DELETE /payments`
  with a `voidReason`, which routes through `SaleSessionPaymentHandler.remove`
  → `SalePaymentService.voidPayments` and marks the payment row voided in
  place. The session keeps the payment with `voidedReason` set, so it stays
  visible to the cashier rather than disappearing from the list.
- **One session per sale.** `SaleSessionHandler.start({saleId})` returns the
  existing session instead of minting a new one; the `by-sale` Redis key is the
  source of truth.
- **Inactive products block commit, not editing.** `SaleSessionValidator.guardNoInactiveProducts` runs in
  `SaleSessionPersister.loadAndValidate` (`saveDraft` and `confirm`), never in `validate()`: a copied or reloaded
  session may legitimately hold inactive lines and must stay loadable so the cashier can remove them. Each line
  response carries `productActive` for the UI. The same guard blocks an existing draft whose product was
  deactivated after it was saved.
- **Walk-in coverage is only enforced at confirm.** The mutation-time validator
  allows under-coverage so the cashier can build up payments incrementally.
- **Discount ceilings ARE enforced on every mutation.** Drafts can no longer
  go upside-down on discounts — this is a behavior change from the pre-session
  model.
- **`SaleSessionRedisStore` is the only class that touches Redis.** Nothing
  else in the app imports `RedisTemplate` / `StringRedisTemplate` — Redis
  access is fully encapsulated behind the `SaleSessionStore` interface.
- **A session line holds its gross `lineTotal`.** `SaleSessionLine` stores `defaultSalePrice` (per product base unit,
  captured on addition from `location_product.default_sale_price`), `baseUnitId` (snapshotted from
  `location_product.base_unit_id`), `storageUnitsPerUserProvidedUnit` (the line unit's size in the storage unit, captured when the
  line is added or its unit changes) and `lineTotal`, loaded from the persisted line and **never recomputed on load**.
  `SaleSessionLineHandler.applyLineChanges` recomputes it with `SaleSessionLine.lineTotal` from the current base unit size
  only when an add or update entry touches the line; confirm trusts the loaded total and the captured ratio and
  reprices nothing. `unitPrice` is derived (`lineTotal ÷ userProvidedQuantity`, half-up, scale 4) and never stored.
  Addition entries do not accept a `unitId`: new lines start at the product's base unit
  (`storageUnitsPerUserProvidedUnit` = the base unit's size). Switching to another unit of the product's group is an update entry;
  a unit outside the group is rejected by `LocationProductDataFetcher.getStorageUnitsPerUserProvidedUnit`. Update entries do **not**
  re-fetch `defaultSalePrice`. `DomainToSessionMapper` copies the persisted `default_sale_price`, `line_total` and
  `storage_units_per_user_provided_unit` straight back onto the line.
  The outbound `SaleSessionLineResponse` keeps its wire fields: `conversionFactor` and `baseQuantity` are display-only
  (`BaseUnitDisplay`: one division from the exact storage figures, `conversionFactor` half-up at 10 decimals, like the unit
  graph's factor, and `baseQuantity` rounded down at 4 decimals, computed with the base unit's current size, so a base-unit line is exactly 1). `lineTotal` is
  the **net** line total (stored gross total plus its adjustments, via `LineMappingContext`), never
  `quantity × netUnitPrice`. The stored gross total is not on the wire under that name. New additive fields:
  `storageQuantityOnHand`, `storageQuantityReserved`, `storageQuantityAvailable` and the breakdown texts.
---

## 9. Price Override

A cashier sets a custom price on a line by supplying `unitPriceOverride` in a
`SaleSessionLineUpdateDto`. The request field is a nullable `Money`: null means "price unchanged", and the client sends it
only when the cashier typed a price. The response always carries `unitPriceOverride`: the typed price when the line has an override, otherwise the
derived list `unitPrice` (for example 55.5556 per Piece, 4 decimals), which is display-only and never sent back. The server owns the adjustment
model — the client never knows about `PRICE_OVERRIDE` adjustments. The quantity is a `PositiveUserProvidedQuantity`, so it is positive with at most 2
decimals (`QuantityLimits`), and a typed price is rejected beyond the money scale when the request is read.

- `PriceOverrideReconciler` (called by `SaleSessionLineHandler.applyLineChanges`)
  stores the typed unit price. The `PRICE_OVERRIDE` adjustment has method `UNIT_PRICE_OVERRIDE` and `value` equal to the
  typed price. Its amount is `|computed line total − override total|`, where the override total is
  `unitPriceOverride × userProvidedQuantity` (`LinePricing.overrideTotal`, rounded once to cents). The signed difference comes
  from `LinePricing.overrideDifference`, shared by the reconciler (direction) and `AdjustmentAmountCalculator` (amount). The amount is recomputed on every
  save, so a quantity edit never touches the stored price. The direction is `DISCOUNT` when the override total is lower and
  `SURCHARGE` when higher; equal totals mean no adjustment. `value` and `direction` are immutable once the adjustment is
  saved, so a new typed price, or a flipped direction, replaces the adjustment under a fresh identity.
  **Unit change clears the override unless a new price is supplied:** when a line's unit changes and the client sends no
  price (or one equal to the existing effective price), the reconciler drops the `PRICE_OVERRIDE`; when the client supplies a different
  `unitPriceOverride`, it is a new explicit override against the updated line. Non-price-override line adjustments are
  always cleared on a unit change: `SaleSessionLineAdjustmentReconciler` drops any remaining line-level adjustment
  whose `relatedSaleLineIdentity` targets a unit-changed line. **No typed price:** a null `unitPriceOverride` keeps the
  existing override. A typed price that equals the current effective price at scale 4 (the override's `value`, otherwise
  the derived list `unitPrice`) is also unchanged. **Typing the list price resets the override:** a typed price is limited to
  the money scale, so a fractional list price (55.5556 per Piece) can only be re-entered rounded (55.56). A typed price that
  equals the updated line's list `unitPrice` at the money scale removes the existing override, or leaves the line without
  one, instead of recording the rounding difference as a surcharge or discount. A deliberate price that differs from the
  rounded list price by a cent or more is still an override.
- A typed `unitPriceOverride == 0` is allowed (cashier giveaway). Negative is
  rejected when the request is deserialized (`Money` is never negative).
- The `PRICE_OVERRIDE` adjustment is stored in Redis just like any other
  adjustment and persisted to the DB at `saveDraft` / `confirm` via
  `SaleAdjustmentSyncer`.
- `unitPriceOverride` on `SaleSessionLineResponse` is always populated:
  `SaleSessionAssembler.buildLineMappingContext` derives it as `(gross ∓ value) ÷ quantity` when an override exists, and
  falls back to the derived `unitPrice` otherwise — never stored in Redis or the DB directly.
- Mutual exclusivity (a line cannot carry both a `PRICE_OVERRIDE` and any
  other adjustment) is enforced structurally inside `validate()` via
  `guardLineOverrideExclusivity`, so it holds regardless of which entry
  point produced the state (line update, adjustment add, load-from-DB). It also keeps a percentage adjustment, whose
  base is the gross total, from stacking on an override.

## 10. Where Things Live

| Topic                           | Class(es)                                                                                                                                                                                                                          |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Session model                   | `SaleSession`, `SaleSessionHeader`, `SaleSessionLine`, `SaleSessionAdjustment`, `SaleSessionPayment`, `SaleSessionTotals`                                                                                                          |
| Identity                        | `SessionIdentity`                                                                                                                                                                                                                  |
| Storage                         | `SaleSessionStore`, `SaleSessionRedisStore`, `SaleSessionInMemoryStore`                                                                                                                                                            |
| Validation                      | `SaleSessionValidator`                                                                                                                                                                                                             |
| Price override reconciliation   | `PriceOverrideReconciler`                                                                                                                                                                                                          |
| Totals                          | `SaleSessionTotalsCalculator`                                                                                                                                                                                                      |
| Load / mint / copy              | `SaleSessionLoader`, `SaleSessionCopier`, `DomainToSessionMapper`                                                                                                                                                                                       |
| Line merge (consolidation)      | `SaleLineMerger`, `PerProductLinesMerger`, `MergedQuantityCalculator`, `MergedAdjustmentsBuilder`, `MergedPriceOverrideBuilder`, `EffectiveLineTotal` (package `line_merge`)                                                          |
| Build response/summary          | `SaleSessionAssembler`, `SessionToResponseMapper`                                                                                                                                                                                  |
| Post-mutation pipeline          | `SaleSessionUpdateFinalizer`                                                                                                                                                                                                       |
| Session orchestration           | `SaleSessionHandler`, `SaleSession{Header,Line,Adjustment,Payment}Handler`, `SaleSessionPersister`                                                                                                                                 |
| Save primitives (in sale/api)   | `SaleSaveRequest`, `SaleSaveResult`, `DraftSalePersister`, `ConfirmedSalePersister`                                                                                                                                                |
| Save sub-pieces                 | `SaleLineSync` (sale internal), `SaleSaveFinalizer` (sale internal), `SaleAdjustmentSyncer` (sale_adjustment/api), `SalePaymentAppender` (sale_payment/api, delegates to `SalePaymentWriter`), `SaleTotalsApplier` (sale internal) |
| Session→SaleSaveRequest mapping | `SessionToSaveRequestMapper`                                                                                                                                                                                                       |
| REST entry point                | `SaleSessionEndpoint` at `/secured/sale-sessions`                                                                                                                                                                                  |

## 11. Line Merge (consolidation)

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

## 12. Pending consolidation session

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
handler writes them to the sale at once through `SaleUpdater.updateNotes` (a draft keeps them in the session until it is
saved) and takes the new sale version into the session. `validate` reads the same table for content: `allowsDuplicateProducts` and the
`ADD_ORDER_LEVEL_ADJUSTMENT` answer. A pending consolidation allows notes, line edits and removals, restoring a removed
line, line-level adjustments, removing adjustments and discarding (void). It refuses adding a line (`ADD_LINE`: the
cashier fixes or removes what is shown and adds products to the generated draft), contact, sold-by, date-sold, order-level
adjustments, payments, save-draft and confirm. Nothing detects a handler that does not call `guardAllowed`; a new handler
must.

**Issues.** `PendingConsolidationAssessor.assess(session, inactiveLocationProductIds, unitConversionGraph)` runs
`SaleLineMerger.merge` on the held lines and passes the problems to `ConsolidationProblemStoryWriter`. The assembler loads
the inactive product ids and the unit graph once per response and hands them to the assessor and to the line mapping.
The pending session also records `cleanProducts` (what combined cleanly at selection, with its source sales, not held on the form);
Generate merges them again, so a clean product that turns inactive afterwards is listed as an issue too
(`ConsolidationProblemStoryWriter.writeInactiveCleanProduct`) and blocks Generate. The assembler includes their ids when it looks up
inactive products. The response carries `kind`, `consolidationIssues` (`saleReferences`, `description`, `fix`) and
`uiOptions.canGenerateConsolidatedSale`, true when no problem remains. The flag only drives the button; Generate re-runs
the check on the server. Every held line has a source sale, since lines cannot be added.

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
it starts. Selecting sales and generating the draft are described in section 13.

## 13. Selecting sales and generating the consolidated draft

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

**After the commit only.** `ConsolidationSessionCleanup` saves the new session to Redis and deletes the open sessions of the
source sales and the pending session in a `TransactionSynchronization.afterCommit`, so a rolled-back Generate never leaves Redis pointing at
a sale that does not exist.

**Order-level adjustments.** Adjustments of the same reason, direction, method and approver become one. An order-level
percentage is money on its own sale (`ConsolidationCopy.orderLevelAmountByAdjustmentKey`, the percentage of that sale's
goods total), so the sources' amounts are added as money and turned back into one percentage of the combined goods total,
only when that percentage is exact (at most 4 decimal places, and it reproduces the same amount). Two 5% discounts on
equal sales stay 5%; a 5% discount on one of two equal sales becomes 2.5%. When it is not exact, Generate is refused and
nothing is voided. One with a different approver stays separate, so an approval is never attributed to an amount its
approver did not see. Notes are joined with "; " without repeats.

