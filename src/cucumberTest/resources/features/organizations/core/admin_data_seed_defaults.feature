Feature: Seeding organization defaults
  As an organization admin
  I want the system defaults seeded into my organization
  So that the expense types, accounts, reasons and units I rely on are always present once and only once

  Background:
    Given I am authenticated as a platform admin

  Scenario Outline: Seeding again keeps each system list at its defaults
    When I POST to /secured/admin-data/seed-defaults
    Then the response status should be 200
    When I GET to <path>
    Then response list size is <count>
    And <table> table should have exactly <count> records in database

    Examples:
      | path                            | table                 | count |
      | /secured/expense-types          | Expense_Type          | 9     |
      | /secured/stock-movement-reasons | Stock_Movement_Reason | 5     |
      | /secured/unitgroups             | Unit_Group            | 4     |
      | /secured/accounts               | Account               | 54    |
      | /secured/contacts               | Contact               | 1     |

  Scenario: Seeding again keeps the stock, adjustment and unit value defaults in the database
    When I POST to /secured/admin-data/seed-defaults
    Then the response status should be 200
    And Stock_Item_Source table should have exactly 6 records in database
    And Adjustment_Reason table should have exactly 11 records in database
    And Unit_Value table should have exactly 9 records in database

  Scenario: Each seeded expense type is linked to its expense account
    When I POST to /secured/admin-data/seed-defaults
    Then the response status should be 200
    When I GET to /secured/expense-types
    Then response contains item with details:
      """json
      {"code": "RNT", "expenseAccountCode": "005.003"}
      """
    And Expense_Type should match example:
      """json
      {"code": "RNT", "expenseAccountCode": "005.003"}
      """
    And Expense_Type table should have exactly 9 records in database

  Scenario: Every seeded expense type is linked to its expense account
    When I POST to /secured/admin-data/seed-defaults
    Then the response status should be 200
    And Expense_Type table should contain exactly these records:
      """json
      [
        {"name": "Wages", "code": "WGS", "expenseAccountCode": "005.002"},
        {"name": "Rent", "code": "RNT", "expenseAccountCode": "005.003"},
        {"name": "Utilities", "code": "UTIL", "expenseAccountCode": "005.004"},
        {"name": "Inbound freight", "code": "INFRT", "expenseAccountCode": "005.005"},
        {"name": "Outbound freight", "code": "OTFRT", "expenseAccountCode": "005.006"},
        {"name": "Office supplies", "code": "OFFSP", "expenseAccountCode": "005.007"},
        {"name": "Staff welfare", "code": "WELF", "expenseAccountCode": "005.008"},
        {"name": "Repairs and maintenance", "code": "RPR", "expenseAccountCode": "005.010"},
        {"name": "Other operating expense", "code": "OTH", "expenseAccountCode": "005.013"}
      ]
      """

  Scenario: Seeded accounts keep their parent links
    When I POST to /secured/admin-data/seed-defaults
    Then the response status should be 200
    When I GET to /secured/accounts
    Then response contains item with details:
      """json
      {"code": "001.001.001", "parentAccountCode": "001.001"}
      """
    And Account should match example:
      """json
      {"code": "001.001.001", "parentAccountCode": "001.001"}
      """
    And Account table should have exactly 54 records in database

  Scenario: Every seeded account keeps its parent link
    When I POST to /secured/admin-data/seed-defaults
    Then the response status should be 200
    And Account table should contain exactly these records:
      """json
      [
        {"code": "001", "parentAccountCode": null},
        {"code": "001.001", "parentAccountCode": "001"},
        {"code": "001.001.001", "parentAccountCode": "001.001"},
        {"code": "001.001.002", "parentAccountCode": "001.001"},
        {"code": "001.001.002.001", "parentAccountCode": "001.001.002"},
        {"code": "001.001.002.002", "parentAccountCode": "001.001.002"},
        {"code": "001.001.003", "parentAccountCode": "001.001"},
        {"code": "001.001.004", "parentAccountCode": "001.001"},
        {"code": "001.001.005", "parentAccountCode": "001.001"},
        {"code": "001.001.006", "parentAccountCode": "001.001"},
        {"code": "001.002", "parentAccountCode": "001"},
        {"code": "001.002.001", "parentAccountCode": "001.002"},
        {"code": "001.002.002", "parentAccountCode": "001.002"},
        {"code": "001.002.003", "parentAccountCode": "001.002"},
        {"code": "001.002.004", "parentAccountCode": "001.002"},
        {"code": "001.002.005", "parentAccountCode": "001.002"},
        {"code": "001.002.006", "parentAccountCode": "001.002"},
        {"code": "002", "parentAccountCode": null},
        {"code": "002.001", "parentAccountCode": "002"},
        {"code": "002.001.001", "parentAccountCode": "002.001"},
        {"code": "002.001.002", "parentAccountCode": "002.001"},
        {"code": "002.002", "parentAccountCode": "002"},
        {"code": "002.003", "parentAccountCode": "002"},
        {"code": "002.004", "parentAccountCode": "002"},
        {"code": "003", "parentAccountCode": null},
        {"code": "003.001", "parentAccountCode": "003"},
        {"code": "003.002", "parentAccountCode": "003"},
        {"code": "003.003", "parentAccountCode": "003"},
        {"code": "003.004", "parentAccountCode": "003"},
        {"code": "004", "parentAccountCode": null},
        {"code": "004.001", "parentAccountCode": "004"},
        {"code": "004.001.001", "parentAccountCode": "004.001"},
        {"code": "004.001.002", "parentAccountCode": "004.001"},
        {"code": "004.001.003", "parentAccountCode": "004.001"},
        {"code": "004.001.004", "parentAccountCode": "004.001"},
        {"code": "004.002", "parentAccountCode": "004"},
        {"code": "005", "parentAccountCode": null},
        {"code": "005.001", "parentAccountCode": "005"},
        {"code": "005.001.001", "parentAccountCode": "005.001"},
        {"code": "005.001.002", "parentAccountCode": "005.001"},
        {"code": "005.002", "parentAccountCode": "005"},
        {"code": "005.003", "parentAccountCode": "005"},
        {"code": "005.004", "parentAccountCode": "005"},
        {"code": "005.005", "parentAccountCode": "005"},
        {"code": "005.006", "parentAccountCode": "005"},
        {"code": "005.007", "parentAccountCode": "005"},
        {"code": "005.008", "parentAccountCode": "005"},
        {"code": "005.009", "parentAccountCode": "005"},
        {"code": "005.010", "parentAccountCode": "005"},
        {"code": "005.011", "parentAccountCode": "005"},
        {"code": "005.012", "parentAccountCode": "005"},
        {"code": "005.013", "parentAccountCode": "005"},
        {"code": "005.014", "parentAccountCode": "005"},
        {"code": "005.015", "parentAccountCode": "005"}
      ]
      """

  Scenario: The walk-in customer is seeded as a contact
    When I POST to /secured/admin-data/seed-defaults
    Then the response status should be 200
    When I GET to /secured/contacts
    Then response contains item with details:
      """json
      {"firstName": "Walk-In Customer"}
      """
    And Contact table should contain exactly these records:
      """json
      [{"firstName": "Walk-In Customer"}]
      """
