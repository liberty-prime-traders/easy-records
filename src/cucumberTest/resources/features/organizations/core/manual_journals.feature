Feature: Manual journals
  As an authenticated organization user
  I want to post balanced manual journals and reverse them
  So that the ledger can be corrected without editing posted documents

  Background:
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" has the organization role "MANAGE_MANUAL_JOURNALS"

  Scenario: A balanced journal is saved with its lines and moves both account balances
    When I POST to /secured/manual-journals with payload:
      """json
      {
        "summary": "Rent accrued against owner's capital",
        "details": "Month-end accrual",
        "postingDate": "{{today}}",
        "lines": [
          {"accountCode": "005.003", "entryType": "DEBIT", "amount": 100.0, "lineMemo": "November rent"},
          {"accountCode": "003.001", "entryType": "CREDIT", "amount": 100.0, "lineMemo": "Capital"}
        ]
      }
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      {
        "summary": "Rent accrued against owner's capital",
        "details": "Month-end accrual",
        "postingDate": "{{today}}",
        "lines": [
          {"accountCode": "003.001", "entryType": "CREDIT", "amount": 100},
          {"accountCode": "005.003", "entryType": "DEBIT", "amount": 100}
        ]
      }
      """
    And response contains details:
      """json
      {"summary": "Rent accrued against owner's capital", "details": "Month-end accrual", "postingDate": "{{today}}"}
      """
    And account "005.003" should have a balance of "100.00"
    And account "003.001" should have a balance of "100.00"
    And Manual_Journal table should contain exactly these records:
      """json
      [{"summary": "Rent accrued against owner's capital"}]
      """
    And Manual_Journal_Line table should contain exactly these records:
      """json
      [
        {"accountCode": "005.003", "entryType": "DEBIT", "amount": 100.0},
        {"accountCode": "003.001", "entryType": "CREDIT", "amount": 100.0}
      ]
      """

  Scenario: A journal without a summary is rejected and nothing is posted
    When I POST to /secured/manual-journals with payload:
      """json
      {
        "summary": "   ",
        "postingDate": "{{today}}",
        "lines": [
          {"accountCode": "005.003", "entryType": "DEBIT", "amount": 100.0},
          {"accountCode": "003.001", "entryType": "CREDIT", "amount": 100.0}
        ]
      }
      """
    Then the response status should be 400
    And the response error message should be "Summary is required."
    And Manual_Journal table should have no records in database
    And account "005.003" should have a balance of "0.00"

  Scenario: A journal with no lines is rejected and nothing is posted
    When I POST to /secured/manual-journals with payload:
      """json
      {"summary": "Empty journal", "postingDate": "{{today}}", "lines": []}
      """
    Then the response status should be 400
    And the response error message should be "At least two lines are required."
    And Manual_Journal table should have no records in database

  Scenario: A journal whose debits and credits differ is rejected and nothing is posted
    When I POST to /secured/manual-journals with payload:
      """json
      {
        "summary": "Unbalanced entry",
        "postingDate": "{{today}}",
        "lines": [
          {"accountCode": "005.003", "entryType": "DEBIT", "amount": 100.0},
          {"accountCode": "003.001", "entryType": "CREDIT", "amount": 90.0}
        ]
      }
      """
    Then the response status should be 400
    And the response error message should be "Debits must equal credits."
    And Manual_Journal table should have no records in database
    And account "005.003" should have a balance of "0.00"

  Scenario Outline: A journal line on an account that cannot be used in a manual journal is rejected
    When I POST to /secured/manual-journals with payload:
      """json
      {
        "summary": "Entry on a restricted account",
        "postingDate": "{{today}}",
        "lines": [
          {"accountCode": "<accountCode>", "entryType": "DEBIT", "amount": 100.0},
          {"accountCode": "003.001", "entryType": "CREDIT", "amount": 100.0}
        ]
      }
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And Manual_Journal table should have no records in database

    Examples:
      | accountCode | error                                                       |
      | 001.001.001 | Account 001.001.001 can't be used in a manual journal.      |
      | 009.999.999 | Account 009.999.999 is not postable.                        |

  Scenario: A journal dated in the future is rejected before any other check
    When I POST to /secured/manual-journals with payload:
      """json
      {
        "summary": "Future entry",
        "postingDate": "2099-01-01",
        "lines": [{"accountCode": "001.001.001", "entryType": "DEBIT", "amount": 100.0}]
      }
      """
    Then the response status should be 400
    And the response error message should be "Posting date cannot be in the future"
    And Manual_Journal table should have no records in database

  Scenario: A journal dated outside every open fiscal period is rejected and nothing is posted
    When I POST to /secured/manual-journals with payload:
      """json
      {
        "summary": "Old entry",
        "postingDate": "2001-01-15",
        "lines": [
          {"accountCode": "005.003", "entryType": "DEBIT", "amount": 100.0},
          {"accountCode": "003.001", "entryType": "CREDIT", "amount": 100.0}
        ]
      }
      """
    Then the response status should be 400
    And the response error message should be "No open fiscal period for 2001-01-15."
    And Manual_Journal table should have no records in database
    And account "005.003" should have a balance of "0.00"

  Scenario Outline: A journal line whose amount is not positive is rejected
    When I POST to /secured/manual-journals with payload:
      """json
      {
        "summary": "Entry with a bad amount",
        "postingDate": "{{today}}",
        "lines": [
          {"accountCode": "005.003", "entryType": "DEBIT", "amount": <amount>},
          {"accountCode": "003.001", "entryType": "CREDIT", "amount": 100.00}
        ]
      }
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And Manual_Journal table should have no records in database

    Examples:
      | amount | error                            |
      | 0.00   | Amount must be greater than zero |
      | -5.00  | Amount must not be negative      |

  Scenario: Reversing a journal posts the opposite lines and links the reversal to the original
    Given a manual journal of "100.00" for rent exists
    When I reverse the last manual journal with reason "Entered against the wrong account"
    Then the response status should be 200
    And the response should reverse the remembered manual journal
    And response contains details with exact lists in order:
      """json
      {
        "summary": "Entered against the wrong account",
        "lines": [
          {"accountCode": "003.001", "entryType": "DEBIT", "amount": 100},
          {"accountCode": "005.003", "entryType": "CREDIT", "amount": 100}
        ]
      }
      """
    And response contains details:
      """json
      {
        "summary": "Entered against the wrong account",
        "reversedByReferenceNumber": null,
        "lines": [
          {"accountCode": "003.001", "entryType": "DEBIT", "amount": 100},
          {"accountCode": "005.003", "entryType": "CREDIT", "amount": 100}
        ]
      }
      """
    And account "005.003" should have a balance of "0.00"
    And account "003.001" should have a balance of "0.00"
    And Manual_Journal table should contain exactly these records:
      """json
      [{"summary": "Rent accrued against owner's capital"}, {"summary": "Entered against the wrong account"}]
      """
    And Manual_Journal_Line table should contain exactly these records:
      """json
      [
        {"accountCode": "005.003", "entryType": "DEBIT", "amount": 100.0},
        {"accountCode": "003.001", "entryType": "CREDIT", "amount": 100.0},
        {"accountCode": "003.001", "entryType": "DEBIT", "amount": 100},
        {"accountCode": "005.003", "entryType": "CREDIT", "amount": 100}
      ]
      """

  Scenario: The journal list marks the original as reversed and the reversal as a reversal
    Given a manual journal of "100.00" for rent exists
    And I reverse the last manual journal with reason "Entered against the wrong account"
    When I GET to /secured/manual-journals
    Then the response status should be 200
    And response contains details:
      """json
      [
        {"summary": "Entered against the wrong account", "reversalOfReferenceNumber": "{{manualJournal}}"},
        {"summary": "Rent accrued against owner's capital", "reversedByReferenceNumber": "{{manualJournalReversal}}"}
      ]
      """
    And Manual_Journal table should contain exactly these records:
      """json
      [{"summary": "Rent accrued against owner's capital"}, {"summary": "Entered against the wrong account"}]
      """

  Scenario: A journal cannot be reversed twice
    Given a manual journal of "100.00" for rent exists
    And I reverse the last manual journal with reason "Entered against the wrong account"
    When I reverse the remembered manual journal again with reason "Second attempt"
    Then the response status should be 400
    And the response error message should be "This manual journal has already been reversed."
    And Manual_Journal table should contain exactly these records:
      """json
      [{"summary": "Rent accrued against owner's capital"}, {"summary": "Entered against the wrong account"}]
      """

  Scenario: A reversal cannot itself be reversed
    Given a manual journal of "100.00" for rent exists
    And I reverse the last manual journal with reason "Entered against the wrong account"
    When I reverse the last manual journal with reason "Reversing the reversal"
    Then the response status should be 400
    And the response error message should be "A reversal cannot be reversed."
    And Manual_Journal table should contain exactly these records:
      """json
      [{"summary": "Rent accrued against owner's capital"}, {"summary": "Entered against the wrong account"}]
      """

  Scenario: A reversal without a reason is rejected and nothing is posted
    Given a manual journal of "100.00" for rent exists
    When I reverse the last manual journal with reason "   "
    Then the response status should be 400
    And the response error message should be "Reason is required."
    And Manual_Journal table should contain exactly these records:
      """json
      [{"summary": "Rent accrued against owner's capital"}]
      """
    And account "005.003" should have a balance of "100.00"

  Scenario: A reversal of an unknown journal is rejected
    When I POST to /secured/manual-journals/MJ-UNKNOWN/reversal with payload:
      """json
      {"reason": "Mistaken entry"}
      """
    Then the response status should be 400
    And the response error message should be "Manual journal MJ-UNKNOWN not found"
    And Manual_Journal table should have no records in database
