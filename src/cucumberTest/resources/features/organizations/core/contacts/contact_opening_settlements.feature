Feature: Contact opening settlements
  As an authenticated organization user
  I want to record payments against a contact's opening balance and void a wrong one
  So that the outstanding opening balance falls with each payment and comes back when a payment is voided

  Background:
    Given a customer named "Acme Traders" exists
    And a payment method named "Cash" on account "001.001.001" exists

  Scenario: Settling part of an opening receivable reduces what is outstanding
    Given the customer has an opening receivable of "100.00"
    When I record a receivable settlement of "40.00" dated "#today"
    Then the response status should be 200
    And response contains details:
      """json
      {"contactReferenceNumber": "#contactReference", "receivableOutstanding": 60, "payableOutstanding": 0,
       "settlements": [{"subledgerDirection": "RECEIVABLE", "paymentAccountCode": "001.001.001",
                        "amount": 40, "settlementDate": "#today"}]}
      """
    And Contact_Opening_Settlement table should have exactly 1 records in database
    And Contact_Opening_Settlement_Void table should have no records in database
    And account "001.001.002.001" should have a balance of "60.00"
    And account "001.001.001" should have a balance of "40.00"

  Scenario: Settling part of an opening payable reduces what we still owe the contact
    Given the customer has an opening payable of "50.00"
    When I record a payable settlement of "30.00" dated "#today"
    Then the response status should be 200
    And response contains details:
      """json
      {"contactReferenceNumber": "#contactReference", "receivableOutstanding": 0, "payableOutstanding": 20,
       "settlements": [{"subledgerDirection": "PAYABLE", "amount": 30}]}
      """
    And Contact_Opening_Settlement table should have exactly 1 records in database
    And account "002.001.001" should have a balance of "20.00"

  Scenario: Settlements are listed newest first
    Given the customer has an opening receivable of "100.00"
    And I record a receivable settlement of "10.00" dated "#today"
    And I record a receivable settlement of "20.00" dated "#today"
    When I GET from secured/contact-opening-settlements with query parameters:
      | contactReferenceNumber | #contactReference |
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      {"receivableOutstanding": 70, "settlements": [{"amount": 20}, {"amount": 10}]}
      """

  Scenario: A contact with an opening balance and no settlements reports the whole balance outstanding
    Given the customer has an opening receivable of "100.00"
    When I GET from secured/contact-opening-settlements with query parameters:
      | contactReferenceNumber | #contactReference |
    Then the response status should be 200
    And response contains details:
      """json
      {"contactReferenceNumber": "#contactReference", "receivableOutstanding": 100, "payableOutstanding": 0}
      """
    And Contact_Opening_Settlement table should have no records in database

  Scenario: Settling more than the outstanding opening receivable is rejected and records nothing
    Given the customer has an opening receivable of "100.00"
    When I record a receivable settlement of "150.00" dated "#today"
    Then the response status should be 400
    And the response error message should be "Settlement of KES 150.00 exceeds the outstanding KES 100.00 receivable opening balance of #contactReference; declare or raise the opening balance first"
    And Contact_Opening_Settlement table should have no records in database
    And account "001.001.002.001" should have a balance of "100.00"
    And account "001.001.001" should have a balance of "0.00"

  Scenario: A settlement above what remains after an earlier settlement is rejected
    Given the customer has an opening receivable of "100.00"
    And I record a receivable settlement of "40.00" dated "#today"
    When I record a receivable settlement of "70.00" dated "#today"
    Then the response status should be 400
    And the response error message should be "Settlement of KES 70.00 exceeds the outstanding KES 60.00 receivable opening balance of #contactReference; declare or raise the opening balance first"
    And Contact_Opening_Settlement table should have exactly 1 records in database
    And account "001.001.002.001" should have a balance of "60.00"

  Scenario: A contact with no declared opening balance cannot be settled
    When I record a receivable settlement of "10.00" dated "#today"
    Then the response status should be 400
    And the response error message should be "Settlement of KES 10.00 exceeds the outstanding KES 0.00 receivable opening balance of #contactReference; declare or raise the opening balance first"
    And Contact_Opening_Settlement table should have no records in database

  Scenario: A settlement dated in the future is rejected
    Given the customer has an opening receivable of "100.00"
    When I record a receivable settlement of "10.00" dated "2999-12-31"
    Then the response status should be 400
    And the response error message should be "Settlement date cannot be in the future"
    And Contact_Opening_Settlement table should have no records in database

  Scenario: A suspended contact cannot be settled
    Given a suspended customer named "Dormant Traders" exists
    When I record a receivable settlement of "10.00" dated "#today"
    Then the response status should be 400
    And the response error message should be "Only an active contact can be settled against an opening balance"
    And Contact_Opening_Settlement table should have no records in database

  Scenario: A settlement of zero is rejected
    Given the customer has an opening receivable of "100.00"
    When I record a receivable settlement of "0.00" dated "#today"
    Then the response status should be 400
    And the response error message should be "Amount must be greater than zero"
    And Contact_Opening_Settlement table should have no records in database

  Scenario: Voiding a settlement puts the settled amount back on the opening receivable
    Given the customer has an opening receivable of "100.00"
    And I record a receivable settlement of "40.00" dated "#today"
    When I void the latest settlement with reason "Entered twice"
    Then the response status should be 200
    And response contains details:
      """json
      {"receivableOutstanding": 100, "payableOutstanding": 0,
       "settlements": [{"amount": 40, "void": {"voidDate": "#today", "reason": "Entered twice"}}]}
      """
    And Contact_Opening_Settlement_Void table should have exactly 1 records in database
    And account "001.001.002.001" should have a balance of "100.00"
    And account "001.001.001" should have a balance of "0.00"

  Scenario: A voided settlement's amount can be settled again
    Given the customer has an opening receivable of "100.00"
    And I record a receivable settlement of "40.00" dated "#today"
    And I void the latest settlement with reason "Wrong account"
    When I record a receivable settlement of "100.00" dated "#today"
    Then the response status should be 200
    And response contains details:
      """json
      {"receivableOutstanding": 0, "payableOutstanding": 0}
      """
    And Contact_Opening_Settlement table should have exactly 2 records in database
    And account "001.001.002.001" should have a balance of "0.00"
    And account "001.001.001" should have a balance of "100.00"

  Scenario: A voided settlement cannot be voided again
    Given the customer has an opening receivable of "100.00"
    And I record a receivable settlement of "40.00" dated "#today"
    And I void the latest settlement with reason "Entered twice"
    When I void the latest settlement with reason "Entered twice"
    Then the response status should be 400
    And the settlement error message should be "Contact opening settlement #settlementReference is already voided"
    And Contact_Opening_Settlement_Void table should have exactly 1 records in database

  Scenario: An opening balance cannot be lowered below the amount already settled against it
    Given the customer has an opening receivable of "100.00"
    And I record a receivable settlement of "40.00" dated "#today"
    When I POST to secured/contact-opening-balances/#contact with payload:
      """json
      {"receivable": 30.00}
      """
    Then the response status should be 400
    And the response error message should be "The receivable opening balance cannot be set to KES 30.00, below the KES 40.00 already settled against it; void the settlements first"
    And Contact_Opening_Settlement table should have exactly 1 records in database
    And account "001.001.002.001" should have a balance of "60.00"
