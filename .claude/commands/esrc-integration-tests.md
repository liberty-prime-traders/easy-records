# Easy Records — Integration Tests (Cucumber)

Apply these rules to every `.feature` file, step class, fixture or hook under `src/cucumberTest`. Read it before adding
a scenario. Unit-test rules are in `/esrc-unit-tests`; the harness reference is `src/cucumberTest/CUCUMBER_TESTING.md`.

---

## Decide first: does this belong here?

A scenario costs a full HTTP round trip, a real Postgres and a per-scenario reset. Ask of every scenario:
**what does this prove that a mocked unit test cannot?** Keep it only if the answer is one of these:

- **Wire contract**: JSON binding, `Optional` update fields, status codes, the error message in the body, `ApiError.body`.
- **Persistence**: the row is really written, and a following `GET` shows it (cache eviction after a write).
- **Database rules**: unique constraints, foreign-key order in a bulk save, cascade and delete order.
- **Seeded data**: behavior that depends on rows every organization starts with (system groups, system units).
- **Cross-domain flow**: one request that provisions another domain's rows (a group creating its root unit).
- **Atomicity**: a rejected request leaves nothing behind.

Everything else is a unit test: a validator branch with no database dependency, a guard clause, a message format,
arithmetic. If a rule is already covered by a `*ValidatorTest`, do not repeat it as a scenario.

Auth: one unauthenticated-403 scenario per feature area is enough. The filter chain is shared by every `/secured` route.

## Before writing: find the unit-test gap

List the tests for the domain (`find src/test -ipath '*<domain>*'`) and read their names against the service. Anything
rule-shaped with no test (a guard in `update`/`delete`, a required-field check) is added as a unit test in the same
change. The two layers split the work: unit tests own rules and branches, integration tests own the seams. A bug that a
new unit test finds is fixed, not skipped.

## Layout

```
src/cucumberTest/resources/features/<area>/<capability>.feature   one capability per file, tag the feature
src/cucumberTest/kotlin/cucumber/steps/<area>/                     step classes, no logic beyond fixtures and ApiClient
src/cucumberTest/kotlin/cucumber/fixtures/<area>/                  FixtureBuilder subclasses (create data through the API)
```

Packages stay `app.myliberty.easy_records.cucumber.*`. Feature names describe business behavior
(`unit_bulk_import.feature`), not endpoints.

## Scenario shape

- The title states the behavior and the outcome: `A rejected payload saves nothing and reports every problem`.
- Every claim in the title is asserted. A title that says a name is freed must reuse the name; one that says nothing
  is saved must read the tables back; one that says a field is kept must show the stored value. Before finishing a
  scenario, reread the title and point at the line that proves each verb in it. If a verb has no line, add the line or
  shorten the title to what is proven.
- Given = setup through fixtures, When = the one request under test, Then = status, then response, then a follow-up
  `GET` when persistence is the point. One behavior per scenario.
- Setup goes through the real endpoints (fixtures), never SQL. The request under test is a raw
  `I POST to /path with payload:` so the wire shape is visible in the feature.
- A scenario that changes data ends with a database check, not only the response: `Then Unit_Group should exist in
  database with id #unitGroup and options:` for an update (a field that must stay, a field that must clear, derived
  columns such as `nameKey`), `should not exist in database with id` for a delete, `Unit_Value should match example:`
  when the id is only in the response. The same check on a rejected request proves the row is unchanged. A GET proves
  the cache, the database step proves the row.
- Business data in the feature, never IDs. Reference created rows with `#unitGroup` (latest) or `#unitGroup->0`.
- Assert the exact message with `the response error message should be "..."`. A bare status check on a 400 proves
  nothing about why it failed. Bulk errors are asserted on `body` with `response contains details:`.
- Assert absence with `response contains no item with details:`. Assert counts with a JSON path
  (`the response field "findAll { it.name == 'Weight' }.size()" should be "1"`), never a list size, because seeded
  system data is always present.
- A rule that holds across many inputs is one `Scenario Outline` with `Examples`, not one scenario per input. Use it for
  matching rules (alphanumeric name keys, case, separators) and for rejected values that share one expected outcome.
  Pick examples that differ in kind: a separator (`pack-size`, `Pack_Size`), case (`PACK SIZE`), no separator
  (`packsize`), trailing punctuation. Do not add rows that exercise the same branch twice. Put the varying value in the
  payload and in the expected message (`<name>`). Examples differing only in the expected message share one table
  column per variable. Every unique-name rule (group name, unit name, bulk group name) gets this treatment, and so does
  every rule over seeded data: one row per seeded system group or unit (`Weight`, `Volume`, `Count`, `Miscellaneous`;
  `g`, `kg`, `hl`, `misc-pc`), because each is a separate row the rule must hold for.
- `{"field": null}` in expected JSON only matches if the response writes the null. Prefer asserting a populated field.

## State and cleaning

Before every scenario `TestHooks` truncates all non-protected tables, then re-creates what a real organization always
has: the `CHART_OF_ACCOUNTS` feature, an open fiscal period, and the output of every `OrgDataSeeder` (system unit groups and units, stock reasons, the Walk-In contact). A scenario
starts from that baseline. Consequences:

- A fixture with a fixed name (`Test Unit Group`) is fine, because the table is empty every scenario.
- A new `OrgDataSeeder` is picked up automatically: the hook calls the seed-defaults endpoint, and the cucumber `AsyncExecutor` runs inline.
  Do not seed in a step.
- The reset is the expensive part. A feature whose scenarios are independent of each other (read-only queries, or
  rejected requests that create their own uniquely named data) may carry `@shared-state`: the first scenario of the
  feature resets, the rest keep that data. Never tag a feature whose Background creates fixed-name data, because the
  second scenario would collide with the first.

## Fixtures and steps

- A fixture builder extends `FixtureBuilder<InsertDto>`, supplies `endpoint` and `defaultDto()`, and its step stores
  the new id under a `TransientKey`. Defaults must pass the validator: outside Miscellaneous a unit needs a base unit,
  so the default unit is based on `#rootUnit`.
- A step creates one kind of thing. Add a named variant (`a unit group named {string} exists`) before adding a table.
- Steps contain no assertions about other domains. Verification of rows belongs in `Then` steps or follow-up requests.

## Gotchas

- Update DTOs use `Optional`, and `applyTo` decides what `null` means. An omitted field always keeps the stored value.
  `StringUtils.useIfProvided` (nullable strings) clears on an explicit `null` or blank; `orElse(existing)` and the
  non-string fields keep the stored value. Read `applyTo` before writing a scenario that expects a field to clear.
- An update validator can still require fields (a unit update needs `name` and `code`). Read the validator before
  writing the payload.
- `testcontainers.reuse` keeps Postgres between runs. A schema or seed change needs the containers removed
  (`docker rm -f -v` of the postgres and kafka containers).
- A request body built from a DTO serializes every public property. A derived property needs `@get:JsonIgnore`.
- Database steps resolve the schema from the entity's package (`SchemaScopedRunner`), so organization and location
  entities work. Entity names are the class name without `Entity`, words joined by `_` (`Unit_Group`).
- A group provisions a root unit whose code is derived from the group's name at creation (`<slug>-base`). Renaming the
  group does not re-derive it, so re-creating the old name needs an explicit `baseUnitCode`. Derived values are fixed at
  creation: before a scenario reuses a name or code, ask what else was derived from it.
- A failing scenario is a finding first: decide whether the product or the scenario is wrong before editing either. Do
  not weaken an assertion to make it pass; retitle the scenario to what is true and add a scenario for the other half.
- Seeded system rows are `systemDefined`: they cannot be modified or deleted, and their names and codes are taken.

## Finish

Run the touched feature, then the whole lane, and read the failure body before editing: a wrong message in the
feature and a wrong rule in the service look the same until the body is read.

```bash
./gradlew cucumberRegressionTest
```
