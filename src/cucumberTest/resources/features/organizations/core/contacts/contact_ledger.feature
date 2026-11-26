Feature: Contact transaction ledger
  As an authenticated organization user
  I want each contact's ledger to list every posting made against it, newest first
  So that what a contact owes or is owed can be traced row by row

  Scenario: A contact that has never transacted has an empty ledger
    Given a customer named "Quiet Customer" exists
    When I POST to /secured/contacts/{{contact}}/ledger with payload:
      """json
      {"previousCursor": "", "requestedSize": 20, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": []}
      """

  Scenario: A loan is listed as a row that subtracts from the balance because the organization owes the lender
    Given a lender named "Ledger Lender" exists
    And a loan of "100.00" from the lender exists
    When I POST to /secured/contacts/{{contact}}/ledger with payload:
      """json
      {"previousCursor": "", "requestedSize": 20, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "contents": [
          {"sourceType": "LOAN", "transactionAmount": 100, "transactionBalanceEffect": "SUBTRACTS", "netBalance": 100}
        ]
      }
      """

  Scenario: A repayment is listed as a row that adds back and leaves the net still owed
    Given a lender named "Repaid Lender" exists
    And a loan of "100.00" from the lender exists
    And a repayment of "60.00" against the loan exists
    When I POST to /secured/contacts/{{contact}}/ledger with payload:
      """json
      {"previousCursor": "", "requestedSize": 20, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "contents": [
          {
            "sourceType": "LOAN_REPAYMENT",
            "transactionAmount": 60,
            "transactionBalanceEffect": "ADDS",
            "netBalance": 40,
            "netBalanceEffect": "SUBTRACTS"
          },
          {"sourceType": "LOAN", "transactionAmount": 100, "netBalance": 100}
        ]
      }
      """

  Scenario: A voided loan is listed as a reversing row that nets the contact's balance to zero
    Given a lender named "Voided Lender" exists
    And a loan of "100.00" from the lender exists
    And the loan has been voided
    When I POST to /secured/contacts/{{contact}}/ledger with payload:
      """json
      {"previousCursor": "", "requestedSize": 20, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "contents": [
          {
            "sourceType": "LOAN_VOID",
            "transactionAmount": 100,
            "transactionBalanceEffect": "ADDS",
            "netBalance": 0,
            "netBalanceEffect": "ZERO"
          },
          {
            "sourceType": "LOAN",
            "transactionAmount": 100,
            "transactionBalanceEffect": "SUBTRACTS",
            "netBalance": 100,
            "netBalanceEffect": "SUBTRACTS"
          }
        ]
      }
      """

  Scenario: An opening receivable is listed as a row that adds because the contact owes the organization
    Given a customer named "Opening Customer" exists
    And a contact opening receivable of "250.00" is declared
    When I POST to /secured/contacts/{{contact}}/ledger with payload:
      """json
      {"previousCursor": "", "requestedSize": 20, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "contents": [
          {
            "sourceType": "CONTACT_OPENING_BALANCE",
            "transactionAmount": 250,
            "transactionBalanceEffect": "ADDS",
            "netBalance": 250,
            "netBalanceEffect": "ADDS"
          }
        ]
      }
      """

  Scenario: An opening payable is listed as a row that subtracts because the organization owes the contact
    Given a customer named "Opening Supplier Account" exists
    And a contact opening payable of "40.00" is declared
    When I POST to /secured/contacts/{{contact}}/ledger with payload:
      """json
      {"previousCursor": "", "requestedSize": 20, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "contents": [
          {
            "sourceType": "CONTACT_OPENING_BALANCE",
            "transactionAmount": 40,
            "transactionBalanceEffect": "SUBTRACTS",
            "netBalance": 40,
            "netBalanceEffect": "SUBTRACTS"
          }
        ]
      }
      """

  Scenario: Opening, loan and repayment rows on one contact carry the running net through each posting
    Given a lender named "Mixed Lender" exists
    And a contact opening receivable of "50.00" is declared
    And a loan of "100.00" from the lender exists
    And a repayment of "30.00" against the loan exists
    When I POST to /secured/contacts/{{contact}}/ledger with payload:
      """json
      {"previousCursor": "", "requestedSize": 20, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "contents": [
          {"sourceType": "LOAN_REPAYMENT", "netBalance": 20, "netBalanceEffect": "SUBTRACTS"},
          {"sourceType": "LOAN", "netBalance": 50, "netBalanceEffect": "SUBTRACTS"},
          {"sourceType": "CONTACT_OPENING_BALANCE", "netBalance": 50, "netBalanceEffect": "ADDS"}
        ]
      }
      """
    And response contains details with exact lists in order:
      """json
      {
        "contents": [
          {
            "sourceType": "LOAN_REPAYMENT",
            "sourceReferenceNumber": "{{repaymentReference}}",
            "transactionAmount": 30,
            "transactionBalanceEffect": "ADDS",
            "netBalance": 20,
            "netBalanceEffect": "SUBTRACTS"
          },
          {
            "sourceType": "LOAN",
            "sourceReferenceNumber": "{{loanReference}}",
            "transactionAmount": 100,
            "transactionBalanceEffect": "SUBTRACTS",
            "netBalance": 50,
            "netBalanceEffect": "SUBTRACTS"
          },
          {
            "sourceType": "CONTACT_OPENING_BALANCE",
            "transactionAmount": 50,
            "transactionBalanceEffect": "ADDS",
            "netBalance": 50,
            "netBalanceEffect": "ADDS"
          }
        ],
        "hasMore": false
      }
      """

  Scenario: The ledger pages newest first and the cursor from one page leads to the next
    Given a lender named "Paged Lender" exists
    And a contact opening receivable of "50.00" is declared
    And a loan of "100.00" from the lender exists
    And a repayment of "30.00" against the loan exists
    When I POST to /secured/contacts/{{contact}}/ledger with payload:
      """json
      {"previousCursor": "", "requestedSize": 2, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"hasMore": true, "contents": [{"sourceType": "LOAN_REPAYMENT"}, {"sourceType": "LOAN"}]}
      """
    And response contains details with exact lists in order:
      """json
      {
        "contents": [
          {"sourceType": "LOAN_REPAYMENT", "transactionAmount": 30, "netBalance": 20},
          {"sourceType": "LOAN", "transactionAmount": 100, "netBalance": 50}
        ],
        "hasMore": true
      }
      """
    When I request the next ledger page of 2 entries
    Then the response status should be 200
    And response contains details:
      """json
      {"hasMore": false, "contents": [{"sourceType": "CONTACT_OPENING_BALANCE"}]}
      """
    And response contains details with exact lists in order:
      """json
      {
        "contents": [{"sourceType": "CONTACT_OPENING_BALANCE", "transactionAmount": 50, "netBalance": 50}],
        "hasMore": false
      }
      """
