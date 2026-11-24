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
already covers a rule, do not repeat it as a scenario. One unauthenticated-403 scenario per feature area is enough.

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
Keep a product folder flat; add a `<domain>` subfolder only when a product folder has enough files to need one. Glue is
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
- Business data in the feature, never IDs. Reference created rows with `#key` (latest) or `#key->0`.
- Assert the exact message (`the response error message should be "..."`). A bare status on a 400 proves nothing about
  why. Bulk errors are asserted on `body` with `response contains details:`.
- Assert absence with `response contains no item with details:`. Where seeded data shares the list, count with a JSON
  path, not a list size.
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
- A failing scenario is a finding first: decide whether the product or the scenario is wrong before editing either. Do
  not weaken an assertion to make it pass.

## Finish

Run the touched feature files (from the IDE), then the whole lane, and read the failure body before editing.
`cucumberRegressionTest` sets its own tag expression, so `-Dcucumber.filter.tags` does not narrow it.

```bash
./gradlew cucumberRegressionTest
```
