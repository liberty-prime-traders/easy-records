# Easy Records — Package Coverage Loop

`/esrc-coverage-loop <domain> [tier]` (for example `account organizations`) verifies one package end to end: sweep, judge the overlap, find
the gaps, close them, re-sweep. Run it package by package. Unit rules are in `/esrc-unit-tests`, scenario rules in
`/esrc-integration-tests`; follow both when writing tests.

Typing this command authorizes the sweep and PIT runs below for that package. No other Gradle, test or cucumber run is
covered by it.

---

## 1. Sweep

1. The argument is the domain folder name and, when the name is not unique, its tier (`platform`, `organizations` or
   `locations`). A full dotted package works too. `verify.sh` resolves the package and picks the `.feature` files named
   after the domain; it prints both. If it finds no features, grep `src/cucumberTest/resources/features` for the domain's
   endpoints and pass them with `--features`, comma-separated.
2. Run `./scripts/verify.sh <domain> [tier] [--features <paths>]`. It prints a verdict, failing tests and the overlap
   table with uncovered line ranges; the full log is `build/verify.log`. Read the log only when the verdict is not green.
3. A failing test that needs infrastructure (Postgres, Kafka unreachable) is an environment problem: report it, do not
   "fix" the code, and carry on with the table.

## 2. Judge the overlap

Cucumber owns wiring: endpoints, persistence, schemas, transactions, auth and cross-domain flows. Unit tests own logic:
branches, validators, calculations and edge cases. Overlap is fine when a file is wiring code both suites must touch
(entities, DTOs, mappers, constants). Flag a file only when all hold:

- 20+ lines, and "both" is 80%+ of its covered lines;
- it holds branching logic, not wiring;
- the cucumber scenarios hitting it assert nothing the unit tests do not.

Report flagged files as a trim candidate list. Never delete tests to cut overlap; deleting is the user's call.

## 3. Find the gaps

For each uncovered range in the table, read the lines and classify:

| Gap | Close it with |
|---|---|
| Branch, validation, calculation, edge case | unit test |
| Endpoint, persistence, query, transaction, auth, event | cucumber scenario (only if a mocked test could not prove it) |
| Wiring hit by one suite only | leave it |
| Unreachable or dead code | report to the user, do not delete |

## 4. Close the gaps

Write the tests, then score every touched feature with `score-feature.sh` and the 10-point rubric; fix anything under
8/10 before moving on. Then re-run the sweep, reusing the unaffected side: `--unit-only` when only unit tests changed,
`--cucumber-only` when only scenarios changed.

## 5. Verify the assertions

Line coverage does not prove assertions. Once the gaps are closed, re-run with `--mutate`: it prints the surviving
mutants by file. Read the line before calling a survivor equivalent: only a mutation that cannot change behavior is
(for example a tie-break inside an inlined `maxOf`). A survivor whose line number is past the end of its source file
is inlined stdlib code (`maxOf`, `single`, `any`): read the call on the line that uses it, and treat it as equivalent
when it only guards a shortcut or an impossible case. A survivor on a guard, a returned value or a removed call is a
missing assertion, so write the test. Ignore survivors in logging, `toString` and generated mappers.

## Stop conditions

- Package line coverage is 95%+ and every remaining gap is classified "leave it", dead code or environment.
- Three sweeps in a row without improvement: stop and report what blocks it.
- After three loops, stop even if gaps remain. Report them.

## Report

End with one short block: coverage before and after, tests added (unit vs scenario), flagged overlap files, PIT
survivors left and why, and anything for the user to decide.
