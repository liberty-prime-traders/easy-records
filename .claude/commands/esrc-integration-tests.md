# Easy Records — Integration Tests (Cucumber)

Apply these rules to every `.feature` file, step class, fixture or hook under `src/cucumberTest`. Read it before adding
a scenario. Unit-test rules are in `/esrc-unit-tests`; the harness reference is `src/cucumberTest/CUCUMBER_TESTING.md`.

---

## Decide first: does this belong here?

A scenario costs an HTTP round trip, a real Postgres and a per-scenario reset. Ask of every scenario:
**what does this prove that a mocked unit test cannot?** Keep it only if the answer is one of these:

- **Wire contract**: JSON binding, `Optional` update fields, status codes, the error message in the body.
- **Persistence**: the row is really written, and a following `GET` shows it (cache eviction after a write).
- **Database rules**: unique constraints, foreign keys, cascade and delete order.
- **Seeded data**: behavior that depends on rows every organization starts with.
- **Cross-domain flow**: one request that provisions or checks another domain's rows.
- **Atomicity**: a rejected request leaves nothing behind.

Everything else is a unit test: a validator branch, a guard clause, a message format, arithmetic. If a `*ValidatorTest`
already covers a rule, do not repeat it as a scenario. Do not write unauthenticated-403 scenarios per feature: authentication is one cross-cutting rule, and a copy per endpoint multiplies with the suite.

## Before writing: find the unit-test gap

List the tests for the domain (`find src/test -ipath '*<domain>*'`) and read their names against the service. A rule with
no test (a guard in `update` or `delete`, a required field) gets a unit test in the same change. Unit tests own rules and
branches, integration tests own the seams. A bug a new test finds is fixed, not skipped.

## Layout

```
src/cucumberTest/resources/features/<tier>/<product>/<capability>.feature   one capability per file
src/cucumberTest/kotlin/cucumber/steps/<tier>/<product>/                    step classes, no logic beyond fixtures and ApiClient
src/cucumberTest/kotlin/cucumber/fixtures/<tier>/<product>/                 FixtureBuilder subclasses (create data through the API)
```

`<tier>` and `<product>` mirror `src/main/.../<tier>/business/<product>` (`organizations/retail`, `organizations/core`).
Keep a product folder flat; give a domain its own subfolder once it has several files of its own (`retail/unit/`
holds the unit group, unit value and bulk-import features, steps and fixtures). Glue is
picked up by package prefix, so a moved step class needs no registration.

Feature names describe business behavior, not endpoints.

## Scenario shape

- The title states the behavior and the outcome. Every claim in it is asserted: a title that says a name is freed
  reuses the name, one that says nothing is saved reads the data back, one that says a field is kept shows the stored
  value. Point at the line that proves each verb in the title, or shorten the title.
- Given = fixtures, When = the one request under test, Then = status, response, then a read-back. One behavior per
  scenario. Setup goes through the real endpoints, never SQL. The request under test is a raw
  `I POST to /path with payload:` so the wire shape is visible.
- A scenario that changes data ends with a database check as well as the response (`should exist in database with id
  ... and options:`, `should not exist ...` for a delete, `should match example:` when the id is only in the response).
  The same check after a rejected request proves the row is unchanged. A GET proves the cache, the database step proves
  the row.
  A row count is not a row check: a scenario that reuses, matches or normalizes data (a spelling that resolves to an
  existing row, a derived key) also asserts the stored value, so "reused" means the stored name is unchanged and no
  second row exists. Outlines and bulk imports follow the same rule.
- Open every JSON doc string as `"""json` so the IDE colors it.
- Business data in the feature, never IDs. Reference created rows with `#key` (latest) or `#key->0`.
- Assert the exact message (`the response error message should be "..."`). A bare status on a 400 proves nothing about
  why. Bulk errors are asserted on `body` with `response contains details:`.
- Assert absence with `response contains no item with details:`. Where seeded data shares the list, count with a JSON
  path, not a list size.
- A table holds only the values that vary, in small columns; the JSON shape stays in the doc string with `<column>`
  placeholders. Rows that need a different payload shape are separate scenarios, not one table of whole payloads.
  Keep a title to what a reader needs to tell scenarios apart; the rest is in the steps.
- A rule that holds across many inputs is one `Scenario Outline`, not one scenario per input. Use it for matching rules
  and for rejected values that share an outcome. Pick rows that differ in kind (separator, case, no separator,
  punctuation), never two rows through the same branch. Put the varying value in the payload and the expected message.
  A rule over seeded data gets one row per seeded row.
- `{"field": null}` in expected JSON only matches if the response writes the null. Prefer a populated field.

## State and cleaning

Before every scenario `TestHooks` truncates all non-protected tables, then re-creates what a real organization has: the
default feature, an open fiscal period and the output of every `OrgDataSeeder` (through the seed-defaults endpoint, with
an inline `AsyncExecutor`). A new seeder is picked up automatically. Do not seed in a step. Scenarios run as the
organization user by default.

- Fixed-name fixtures are fine, because the tables are empty every scenario.
- The reset is the expensive part. A feature whose scenarios are independent may carry `@shared-state`: the first scenario
  resets, the rest keep the data. Never tag a feature whose Background creates fixed-name data.

## Fixtures and steps

- A fixture builder extends `FixtureBuilder<InsertDto>`, supplies `endpoint` and `defaultDto()`, and its step stores the
  new id under a `TransientKey`. Defaults must pass the validator.
- Prefer selecting a seeded row over creating a chain of prerequisites just to get an id.
- A step creates one kind of thing. Add a named variant before adding a table.
- Name the fixtures a scenario asserts on (`a product group named "Dairy" exists`) instead of `a product group exists`
  plus a default-name expectation: the feature then states its own data.
- Steps contain no assertions about other domains. Row checks belong in `Then` steps or follow-up requests.

## Gotchas

- Update DTOs use `Optional`, and `applyTo` decides what `null` means. An omitted field keeps the stored value;
  `StringUtils.useIfProvided` clears a string on explicit `null` or blank. Read `applyTo` before writing a clearing scenario.
- An update validator can still require fields. Read it before writing the payload.
- Values derived at creation (codes, keys) are not re-derived on rename. Before a scenario reuses a name or code, ask
  what else was derived from it.
- A request body built from a DTO serializes every public property. A derived property needs `@get:JsonIgnore`.
- Database steps pick the schema from the entity's package. Entity names are the class name without `Entity`, words
  joined by `_`.
- `testcontainers.reuse` keeps Postgres between runs. A schema or seed change needs the containers removed.
- Search services return every row and ignore the text while the table is at or below
  `esr.product-search.client-side-filter-threshold`. The harness pins it to 0 so scenarios reach the SQL.
- The first page of a paged search sends `"previousCursor": ""`. The query builders take a non-null cursor, so `null`
  throws a NullPointerException.
- Read the endpoint before asserting its status: the success code is whatever `@ResponseStatus` or the return type
  says (a `Unit` handler is 200 unless annotated), never an assumption from another endpoint.
- The reset removes organization membership, so the default organization user is denied on member-gated endpoints
  (`isOrganizationMember`). A step that needs a lookup the user cannot make goes through a platform-admin endpoint, then
  restores the default user.
- A scenario that waits on Kafka asserts on state the consumer wrote, polled with Awaitility. A timeout has three
  suspects, in this order: the event left before the commit (publish only `AFTER_COMMIT`), a cache that lives outside
  the cache manager (the harness clears all through `CacheClearService`), the consumer threw (read its log first).
- A step that fails to parse a response hides the cause. Check the status first and put the body in the failure message.
- A failing scenario is a finding first: decide whether the product or the scenario is wrong before editing either. Do
  not weaken an assertion to make it pass.

## Score before running

Read `.claude-local/integration-tests/gotchas.md`: it holds the gotchas learned so far and a 10-point score. Run
`.claude-local/integration-tests/score-feature.sh <feature>`, score each touched feature by hand for the rest, and fix anything under
8/10 before it runs. An expected status or message that does not match the code fails the run, so those two items gate
the score. When a scenario fails for a reason that applies beyond it, add a line to that file and grep the suite for the
same mistake before declaring it fixed.

## Finish

Run the touched feature files (from the IDE), then the whole lane, and read the failure body before editing.
`cucumberRegressionTest` sets its own tag expression, so `-Dcucumber.filter.tags` does not narrow it.

```bash
./gradlew cucumberRegressionTest
```
