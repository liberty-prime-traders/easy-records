Feature: Voiding loans and repayments
  As an authenticated organization user
  I want to void a wrong loan or repayment
  So that corrections reverse the ledger without losing the record

  Background:
    Given a lender named "First Bank" exists
    And a loan of "100.00" from the lender exists

  Scenario: Voiding a loan takes the whole amount back out of loans payable and cash
    When I POST to /secured/loans/void with payload:
      """json
      {"loanId": "#loan", "reason": "Entered twice"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"status": "VOIDED", "amountRepaid": 0, "outstanding": 0, "void": {"voidDate": "#today", "reason": "Entered twice"}}
      """
    And Loan_Void should match example:
      """json
      {"loanId": "#loan", "voidDate": "#today", "reason": "Entered twice"}
      """
    And account "002.004" should have a balance of "0.00"
    And account "001.001.001" should have a balance of "0.00"
    And loans payable should equal the sum of outstanding loans

  Scenario: A loan with an active repayment cannot be voided
    Given a repayment of "10.00" against the loan exists
    When I POST to /secured/loans/void with payload:
      """json
      {"loanId": "#loan"}
      """
    Then the response status should be 400
    And the response error message should be "Loan #loanReference cannot be voided while it has repayment: #repaymentReference"
    And Loan_Void table should have no records in database
    And account "002.004" should have a balance of "90.00"

  Scenario: A loan can be voided once its repayment has been voided
    Given a repayment of "10.00" against the loan exists
    And the repayment has been voided
    When I POST to /secured/loans/void with payload:
      """json
      {"loanId": "#loan"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"status": "VOIDED", "outstanding": 0}
      """
    And Loan_Void table should have exactly 1 records in database
    And account "002.004" should have a balance of "0.00"
    And account "001.001.001" should have a balance of "0.00"
    When I GET to /secured/contacts
    Then response contains item with details:
      """json
      {"referenceNumber": "#contactReference", "payableBalance": 110, "receivableBalance": 110}
      """

  Scenario: A voided loan cannot be voided again
    Given the loan has been voided
    When I POST to /secured/loans/void with payload:
      """json
      {"loanId": "#loan"}
      """
    Then the response status should be 400
    And the response error message should be "Loan #loanReference is already voided"
    And Loan_Void table should have exactly 1 records in database

  Scenario: Voiding a repayment reopens a settled loan
    Given a repayment of "100.00" against the loan exists
    When I POST to /secured/loans/repayments/void with payload:
      """json
      {"repaymentId": "#repayment", "reason": "Wrong account"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "status": "OPEN",
        "amountRepaid": 0,
        "outstanding": 100,
        "repayments": [{"amount": 100, "void": {"voidDate": "#today", "reason": "Wrong account"}}]
      }
      """
    And Loan_Repayment_Void should match example:
      """json
      {"loanRepaymentId": "#repayment", "voidDate": "#today", "reason": "Wrong account"}
      """
    And account "002.004" should have a balance of "100.00"
    And loans payable should equal the sum of outstanding loans

  Scenario: A repayment cannot be voided twice
    Given a repayment of "10.00" against the loan exists
    And the repayment has been voided
    When I POST to /secured/loans/repayments/void with payload:
      """json
      {"repaymentId": "#repayment"}
      """
    Then the response status should be 400
    And the response error message should be "Loan repayment #repaymentReference is already voided"
    And Loan_Repayment_Void table should have exactly 1 records in database
