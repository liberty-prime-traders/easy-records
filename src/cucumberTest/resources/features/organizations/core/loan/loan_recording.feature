Feature: Recording a loan
  As an authenticated organization user
  I want to record money borrowed from a lender
  So that loans payable shows what the business owes

  Scenario: A loan is saved and raises loans payable, the cash account and the lender's payable balance
    Given a lender named "First Bank" exists
    When I POST to /secured/loans with payload:
      """json
      {
        "lenderContactReference": "#contactReference",
        "assetAccountCode": "001.001.001",
        "amount": 100.00,
        "loanDate": "#today",
        "note": "Working capital"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "lender": {"displayName": "First Bank"},
        "assetAccount": {"code": "001.001.001"},
        "amount": 100,
        "note": "Working capital",
        "status": "OPEN",
        "amountRepaid": 0,
        "outstanding": 100
      }
      """
    And Loan should match example:
      """json
      {
        "lenderContactId": "#contact",
        "assetAccountCode": "001.001.001",
        "amount": 100,
        "loanDate": "#today",
        "note": "Working capital"
      }
      """
    And account "002.004" should have a balance of "100.00"
    And account "001.001.001" should have a balance of "100.00"
    When I GET to /secured/contacts
    Then response contains item with details:
      """json
      {"referenceNumber": "#contactReference", "payableBalance": 100, "receivableBalance": 0}
      """
    And loans payable should equal the sum of outstanding loans

  Scenario: A loan from a contact that is not a lender is rejected
    Given a customer named "Plain Customer" exists
    When I POST to /secured/loans with payload:
      """json
      {
        "lenderContactReference": "#contactReference",
        "assetAccountCode": "001.001.001",
        "amount": 100.00,
        "loanDate": "#today"
      }
      """
    Then the response status should be 400
    And the response error message should be "Contact Plain Customer is not a lender"
    And Loan table should have no records in database

  Scenario: A loan from an unknown lender is rejected
    When I POST to /secured/loans with payload:
      """json
      {"lenderContactReference": "CT999999", "assetAccountCode": "001.001.001", "amount": 100.00, "loanDate": "#today"}
      """
    Then the response status should be 400
    And the response error message should be "Lender not found: CT999999"
    And Loan table should have no records in database

  Scenario: A loan without a lender is rejected
    When I POST to /secured/loans with payload:
      """json
      {"lenderContactReference": " ", "assetAccountCode": "001.001.001", "amount": 100.00, "loanDate": "#today"}
      """
    Then the response status should be 400
    And the response error message should be "Lender is required"
    And Loan table should have no records in database

  Scenario Outline: A loan into an account that cannot receive the money is rejected
    Given a lender named "First Bank" exists
    When I POST to /secured/loans with payload:
      """json
      {
        "lenderContactReference": "#contactReference",
        "assetAccountCode": "<account>",
        "amount": 100.00,
        "loanDate": "#today"
      }
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And Loan table should have no records in database

    Examples:
      | account     | error                                                                  |
      | 001.001.003 | System-defined loan account must be a direct child of Digital Payments |
      | 002.004     | Loan account must be of type Asset                                     |
      | 009.999.999 | Loan account not found: 009.999.999                                    |

  Scenario Outline: A loan whose amount is not positive is rejected
    Given a lender named "First Bank" exists
    When I POST to /secured/loans with payload:
      """json
      {
        "lenderContactReference": "#contactReference",
        "assetAccountCode": "001.001.001",
        "amount": <amount>,
        "loanDate": "#today"
      }
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And Loan table should have no records in database

    Examples:
      | amount | error                           |
      | 0.00   | Amount must be greater than zero |
      | -5.00  | Amount must not be negative      |

  Scenario: A loan dated outside every open fiscal period leaves nothing behind
    Given a lender named "First Bank" exists
    When I POST to /secured/loans with payload:
      """json
      {
        "lenderContactReference": "#contactReference",
        "assetAccountCode": "001.001.001",
        "amount": 100.00,
        "loanDate": "2001-01-15"
      }
      """
    Then the response status should be 400
    And the response error message should be "No open fiscal period for 2001-01-15"
    And Loan table should have no records in database
    And no loan ledger groups should exist

  Scenario: Loans payable takes no manual opening balance
    When I POST to /secured/opening-balances with payload:
      """json
      {"accountCode": "002.004", "newAmount": 10}
      """
    Then the response status should be 400
    And the response error message should be "Account 002.004 is driven only by loans and cannot take a manual opening balance"
