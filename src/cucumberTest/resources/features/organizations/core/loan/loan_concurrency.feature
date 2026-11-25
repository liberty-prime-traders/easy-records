Feature: Concurrent loan requests
  As an organization
  I want simultaneous requests on one loan to stay consistent
  So that loans payable never drifts from what is outstanding

  Background:
    Given a lender named "First Bank" exists

  Scenario: Two simultaneous repayments that together exceed the outstanding amount cannot both succeed
    When 5 rounds of two simultaneous repayments of "60.00" are submitted against fresh loans of "100.00"
    Then exactly one request should succeed in every round
    And loans payable should equal the sum of outstanding loans

  Scenario: A repayment racing a loan void cannot both succeed
    When 5 rounds of a repayment of "10.00" racing a loan void are submitted against fresh loans of "100.00"
    Then exactly one request should succeed in every round
    And loans payable should equal the sum of outstanding loans
