# easy-records
## Verifying a package

```bash
./scripts/verify.sh account organizations            # unit + cucumber coverage for one package, with the overlap table
./scripts/verify.sh account organizations --mutate   # plus PIT mutation testing against the unit tests
```

The domain is its folder name; the tier (`platform`, `organizations`, `locations`) is needed only when the name appears in
more than one. A full dotted package works too. Reports land in `build/reports/coverage/{unit,integration,merged}/html`
and `build/reports/pitest`. In Claude Code, `/esrc-coverage-loop account organizations` runs the same check and then
writes the tests for the gaps it finds.
