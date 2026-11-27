Feature: Searching loans
  As an authenticated organization user
  I want to find loans by lender and status
  So that I can see what each lender is still owed

  Background:
    Given a lender named "First Bank" exists
    And a loan of "100.00" from the lender exists
    And a loan of "50.00" from the lender exists
    And a repayment of "50.00" against the loan exists

  Scenario Outline: A lender's loans are listed by status
    When I search the loans created in the last day with parameters:
      """json
      {"lenderContactReferences": ["{{contactReference}}"], "statuses": ["<status>"]}
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": <loans>}
      """

    Examples:
      | status  | loans                                           |
      | OPEN    | [{"amount": 100, "status": "OPEN"}]             |
      | SETTLED | [{"amount": 50, "status": "SETTLED"}]           |
      | VOIDED  | []                                              |

  Scenario: Searching for an unknown lender finds nothing
    When I search the loans created in the last day with parameters:
      """json
      {"lenderContactReferences": ["CT999999"]}
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": []}
      """

  Scenario: A search without a created range is rejected
    When I POST to /secured/loans/search with payload:
      """json
      {"parameters": {}, "previousCursor": "", "requestedSize": 50}
      """
    Then the response status should be 400
    And the response error message should be "createdFrom and createdBefore are required"

  Scenario: A search over more than a year is rejected
    When I POST to /secured/loans/search with payload:
      """json
      {
        "parameters": {"createdFrom": "2020-01-01T00:00:00Z", "createdBefore": "2023-01-01T00:00:00Z"},
        "previousCursor": "",
        "requestedSize": 50
      }
      """
    Then the response status should be 400
    And the response error message should be "createdFrom and createdBefore must be at most 1 year apart"

  Scenario: A loan is fetched by its reference with its lender and repayment
    When I GET to /secured/loans/{{loanReference}}
    Then the response status should be 200
    And response contains details:
      """json
      {"amount": 50, "outstanding": 0, "status": "SETTLED", "lender": {"displayName": "First Bank"}}
      """

  Scenario: Fetching a loan by an unknown reference is rejected
    When I GET to /secured/loans/LOAN999999
    Then the response status should be 400
    And the response error message should be "Loan not found: LOAN999999"
