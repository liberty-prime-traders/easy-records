Feature: Seeding organization defaults
  As an organization admin
  I want the system defaults seeded into my organization
  So that the expense types, accounts, reasons and units I rely on are always present once and only once

  Scenario Outline: Seeding again keeps each system list at its defaults
    When I POST to/from /secured/admin-data/seed-defaults
    Then the response status should be 200
    When I GET to/from <path>
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
    When I POST to/from /secured/admin-data/seed-defaults
    Then the response status should be 200
    And Stock_Item_Source table should have exactly 6 records in database
    And Adjustment_Reason table should have exactly 11 records in database
    And Unit_Value table should have exactly 9 records in database

  Scenario: Each seeded expense type is linked to its expense account
    When I POST to/from /secured/admin-data/seed-defaults
    Then the response status should be 200
    When I GET to/from /secured/expense-types
    Then response contains item with details:
      """json
      {"code": "RNT", "name": "Rent", "expenseAccountCode": "005.003", "systemDefined": true}
      """
    And Expense_Type table should have exactly 9 records in database

  Scenario: Seeded accounts keep their parent links
    When I POST to/from /secured/admin-data/seed-defaults
    Then the response status should be 200
    When I GET to/from /secured/accounts
    Then response contains item with details:
      """json
      {"code": "001.001.001", "name": "Cash", "parentAccountCode": "001.001"}
      """
    And Account table should have exactly 54 records in database

  Scenario: The walk-in customer is seeded as a contact
    When I POST to/from /secured/admin-data/seed-defaults
    Then the response status should be 200
    When I GET to/from /secured/contacts
    Then response contains item with details:
      """json
      {"firstName": "Walk-In Customer"}
      """
    And Contact table should have exactly 1 records in database
