Feature: Contact opening balance
  As an authenticated organization user
  I want to declare a contact's opening receivable and payable
  So that the contact list reports the latest revision per subledger direction

  Scenario: The listed opening balance follows the latest revision of each direction
    Given an opening balance contact exists
    When I set the contact's opening balances to payable "40.00" and receivable "100.00"
    And I set the contact's opening balances to payable "40.00" and receivable "90.00"
    Then the response status should be 200
    And the contact's listed net opening balance should be "50.00" with effect "ADDS"
    And the contact's listed balances should be receivable "100.00" and payable "50.00"
    And Contact_Opening_Balance table should have exactly 3 records in database

  Scenario: A downward payable revision flips the net opening effect and posts the reduction to the receivable column
    Given an opening balance contact exists
    When I set the contact's opening balances to payable "40.00" and receivable "0.00"
    And I set the contact's opening balances to payable "25.00" and receivable "0.00"
    Then the response status should be 200
    And the contact's listed net opening balance should be "25.00" with effect "SUBTRACTS"
    And the contact's listed balances should be receivable "15.00" and payable "40.00"
    And the contact's opening balance history should be:
      | subledgerDirection | amount | balanceEffect |
      | PAYABLE            | 40.00  | SUBTRACTS     |
      | PAYABLE            | 25.00  | SUBTRACTS     |
    And Contact_Opening_Balance table should have exactly 2 records in database

  Scenario: An upward receivable revision raises the listed receivable and keeps the net effect ADDS
    Given an opening balance contact exists
    When I set the contact's opening balances to payable "0.00" and receivable "100.00"
    And I set the contact's opening balances to payable "0.00" and receivable "150.00"
    Then the response status should be 200
    And the contact's listed net opening balance should be "150.00" with effect "ADDS"
    And the contact's listed balances should be receivable "150.00" and payable "0.00"
    And the contact's opening balance history should be:
      | subledgerDirection | amount | balanceEffect |
      | RECEIVABLE         | 100.00 | ADDS          |
      | RECEIVABLE         | 150.00 | ADDS          |
    And Contact_Opening_Balance table should have exactly 2 records in database

  Scenario: Revising a receivable to zero clears the net opening effect and keeps the earlier postings in the listed balances
    Given an opening balance contact exists
    When I set the contact's opening balances to payable "0.00" and receivable "100.00"
    And I set the contact's opening balances to payable "0.00" and receivable "0.00"
    Then the response status should be 200
    And the contact's listed net opening balance should be "0.00" with effect "ZERO"
    And the contact's listed balances should be receivable "100.00" and payable "100.00"
    And the contact's opening balance history should be:
      | subledgerDirection | amount | balanceEffect |
      | RECEIVABLE         | 100.00 | ADDS          |
      | RECEIVABLE         | 0.00   | ZERO          |
    And Contact_Opening_Balance table should have exactly 2 records in database

  Scenario: Re-declaring an unchanged balance records no revision
    Given an opening balance contact exists
    When I set the contact's opening balances to payable "0.00" and receivable "100.00"
    And I set the contact's opening balances to payable "0.00" and receivable "100.00"
    Then the response status should be 200
    And the contact's opening balance history should be:
      | subledgerDirection | amount | balanceEffect |
      | RECEIVABLE         | 100.00 | ADDS          |
    And Contact_Opening_Balance table should have exactly 1 records in database

  Scenario: The history lists revisions oldest first across both directions
    Given an opening balance contact exists
    When I set the contact's opening balances to payable "0.00" and receivable "100.00"
    And I set the contact's opening balances to payable "40.00" and receivable "100.00"
    And I set the contact's opening balances to payable "40.00" and receivable "90.00"
    Then the response status should be 200
    And the contact's opening balance history should be:
      | subledgerDirection | amount | balanceEffect |
      | RECEIVABLE         | 100.00 | ADDS          |
      | PAYABLE            | 40.00  | SUBTRACTS     |
      | RECEIVABLE         | 90.00  | ADDS          |
    And Contact_Opening_Balance table should have exactly 3 records in database

  Scenario Outline: A malformed opening balance is rejected and leaves no revision
    Given an opening balance contact exists
    When I set the contact's opening balances to payable "<payable>" and receivable "<receivable>"
    Then the response status should be 400
    And the response error message should be "<message>"
    And Contact_Opening_Balance table should have no records in database
    And the contact's listed net opening balance should be "0.00" with effect "ZERO"

    Examples:
      | payable          | receivable | message                                     |
      | 0.00             | -1.00      | Amount must not be negative                 |
      | 1.001            | 0.00       | Amount can have at most 2 decimal places    |
      | 1000000000000.00 | 0.00       | Amount must not exceed 999999999999.99      |

  Scenario: An inactive contact cannot take an opening balance
    Given an inactive opening balance contact exists
    When I set the contact's opening balances to payable "10.00" and receivable "0.00"
    Then the response status should be 400
    And the response error message should be "Only an active contact can take an opening balance"
    And Contact_Opening_Balance table should have no records in database

  Scenario: A revision of a contact that already has an opening balance is measured against the declared figure
    Given a contact with an opening balance of payable "40.00" and receivable "100.00"
    When I set the contact's opening balances to payable "40.00" and receivable "120.00"
    Then the response status should be 200
    And the contact's listed net opening balance should be "80.00" with effect "ADDS"
    And the contact's listed balances should be receivable "120.00" and payable "40.00"
    And Contact_Opening_Balance table should have exactly 3 records in database
