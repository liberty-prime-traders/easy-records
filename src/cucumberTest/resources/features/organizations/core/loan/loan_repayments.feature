Feature: Repaying a loan
  As an authenticated organization user
  I want to record repayments against a loan
  So that what the business still owes is always known

  Background:
    Given a lender named "First Bank" exists
    And a loan of "100.00" from the lender exists

  Scenario: A repayment lowers the outstanding amount and the cash account
    When I POST to /secured/loans/repayments with payload:
      """json
      {
        "loanId": "{{loan}}",
        "assetAccountCode": "001.001.001",
        "amount": 60.0,
        "repaymentDate": "{{today}}",
        "note": "First instalment"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"outstanding": 40, "amountRepaid": 60, "repayments": [{"amount": 60}]}
      """
    And Loan_Repayment should match example:
      """json
      {"loanId": "{{loan}}", "assetAccountCode": "001.001.001", "amount": 60}
      """
    And account "002.004" should have a balance of "40.00"
    And account "001.001.001" should have a balance of "40.00"
    When I GET to /secured/contacts
    Then response contains item with details:
      """json
      {"referenceNumber": "{{contactReference}}", "payableBalance": 100, "receivableBalance": 60}
      """
    And loans payable should equal the sum of outstanding loans

  Scenario: The repayment that clears the outstanding amount settles the loan
    Given a repayment of "60.00" against the loan exists
    When I POST to /secured/loans/repayments with payload:
      """json
      {"loanId": "{{loan}}", "assetAccountCode": "001.001.001", "amount": 40.0, "repaymentDate": "{{today}}"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"outstanding": 0, "status": "SETTLED", "repayments": [{"amount": 40}]}
      """
    And Loan_Repayment table should have exactly 2 records in database
    And account "002.004" should have a balance of "0.00"
    And loans payable should equal the sum of outstanding loans

  Scenario: A repayment above the outstanding amount is rejected and names both amounts
    Given a repayment of "70.00" against the loan exists
    When I POST to /secured/loans/repayments with payload:
      """json
      {"loanId": "{{loan}}", "assetAccountCode": "001.001.001", "amount": 30.01, "repaymentDate": "{{today}}"}
      """
    Then the response status should be 400
    And the response error message should be "Repayment of KES 30.01 exceeds the outstanding KES 30.00 on loan {{loanReference}}"
    And Loan_Repayment table should have exactly 1 records in database
    And account "002.004" should have a balance of "30.00"

  Scenario: A repayment dated before the loan is rejected
    When I POST to /secured/loans/repayments with payload:
      """json
      {"loanId": "{{loan}}", "assetAccountCode": "001.001.001", "amount": 10.0, "repaymentDate": "2001-01-15"}
      """
    Then the response status should be 400
    And the response error message should be "Repayment date cannot be before the loan date {{today}}"
    And Loan_Repayment table should have no records in database
    And account "002.004" should have a balance of "100.00"

  Scenario: A voided loan accepts no repayment
    Given the loan has been voided
    When I POST to /secured/loans/repayments with payload:
      """json
      {"loanId": "{{loan}}", "assetAccountCode": "001.001.001", "amount": 10.0, "repaymentDate": "{{today}}"}
      """
    Then the response status should be 400
    And the response error message should be "Loan {{loanReference}} is voided"
    And Loan_Repayment table should have no records in database

  Scenario: A repayment against a loan that does not exist is rejected
    When I POST to /secured/loans/repayments with payload:
      """json
      {
        "loanId": "00000000-0000-0000-0000-000000000000",
        "assetAccountCode": "001.001.001",
        "amount": 10.0,
        "repaymentDate": "{{today}}"
      }
      """
    Then the response status should be 400
    And the response error message should be "Loan not found"
    And Loan_Repayment table should have no records in database

  Scenario: A repayment from an account that cannot hold money is rejected
    When I POST to /secured/loans/repayments with payload:
      """json
      {"loanId": "{{loan}}", "assetAccountCode": "001.001.003", "amount": 10.0, "repaymentDate": "{{today}}"}
      """
    Then the response status should be 400
    And the response error message should be "System-defined loan account must be a direct child of Digital Payments"
    And Loan_Repayment table should have no records in database

  Scenario: A repayment of zero is rejected
    When I POST to /secured/loans/repayments with payload:
      """json
      {"loanId": "{{loan}}", "assetAccountCode": "001.001.001", "amount": 0.0, "repaymentDate": "{{today}}"}
      """
    Then the response status should be 400
    And the response error message should be "Amount must be greater than zero"
    And Loan_Repayment table should have no records in database
