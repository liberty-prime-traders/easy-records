# RTSS — Unit Tests

Apply these rules to every test written or edited in this session. Read it before adding a test file, changing a
constructor or data class that tests build, or fixing a failing test.

---

## Warnings are banned

A test file compiles with no warnings. The ones that keep appearing:

- **Unnecessary non-null assertion (`!!`).** Use `!!` only on a type that is really nullable (entity ids, optional
  fields). `RtsGenericException.message` is a non-null `String`: `refusal.message.contains("SALE02")`, never
  `refusal.message!!.contains("SALE02")`.
- **Unnecessary safe call (`?.`)** on a non-null receiver.
- **Condition that is always true or false**, and **unused variable, parameter or import**.

Before finishing, run `./gradlew compileTestKotlin --rerun-tasks 2>&1 | grep "^w:"`. Nothing may print for a file you
touched. Fix the type or the call, never suppress the warning.

## Layout

- A test mirrors the package of the class it tests and is named `<Class>Test`. Several tests of one class split by
  behavior, e.g. `SaleSessionCopierContentTest`.
- Shared builders and stubs for a domain go in one `<Domain>TestSupport` file (see `LoanTestSupport`). A unit test is
  plain JUnit 5 with Mockito. Only the integration tests (`AbstractIntegrationTest`) start Spring; do not add `@SpringBootTest` to a unit test.

## Names and shape

- Test names are backticked sentences that state the behavior, not the method:
  `` `a sale that holds two lines of the product is named once` ``.
- One behavior per test. Build objects in small private helpers (`saleLine("2")`), not in every test.
- Verbose names and no star imports, as in `/rtss-conventions`: `consolidationCopy`, not `copy`.

## Assertions

- A refusal test captures the exception and asserts its message, not only that something was thrown:

  ```kotlin
  val refusal = assertThrows(RtsGenericException::class.java) { generator.generate(staleInput) }
  assertTrue(refusal.message.contains("SALE02"))
  ```

- Compare `BigDecimal` with `compareTo`: `assertEquals(0, BigDecimal("12.50").compareTo(actual))`. `assertEquals` on two
  `BigDecimal`s fails on scale (`12.5` vs `12.50`).
- A guard test proves three things: the message, that nothing was written (`verifyNoInteractions(...)` or a
  `verifyNothingWasChanged()` helper), and that the allowed neighbour still passes. A refusal that half-wrote data is
  the bug the test exists to catch.

## Mocks and matchers

- Mock collaborators with `mock(X::class.java)`. Never mock the class under test or a plain data class; build a real one.
- Mockito matchers return null, which Kotlin rejects for non-null parameters, so use `anyNonNull` and `eqNonNull`. Use the
  ones in the domain's `*TestSupport` when it has them; otherwise a private helper in the file is the norm.

## Test data

- Build real objects with the production builders where they exist (`SaleSessionBuilder`) and override with `copy(...)`.
- When a constructor or data class gains a field, update every construction in the same change:
  `grep -rn "ClassName(" src/test`. A test that stays red only because of a missing argument says nothing about behavior.
- A session or entity stamps the time it was built, so two builds of the same data are never equal. Build once per test
  (`by lazy`), then compare that instance.

## Comments

- A test comment names the invariant being guarded and what breaks if it regresses, not the bug or task that prompted
  the test. Most tests need none.

## Source of truth

Full standards: `.claude/instructions.md`
