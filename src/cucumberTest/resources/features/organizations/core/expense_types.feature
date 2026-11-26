Feature: Expense types
  As an authenticated organization user
  I want to maintain the expense categories
  So that every expense posts to the right expense account and is only used where it is allowed

  Scenario: The nine seeded system expense types are listed, Rent with its account
    When I GET to /secured/expense-types
    Then the response status should be 200
    And response contains details:
      """json
      [
        {"code": "WGS"},
        {"code": "RNT", "name": "Rent", "expenseAccountCode": "005.003"},
        {"code": "UTIL", "name": "Utilities"},
        {"code": "INFRT", "name": "Inbound freight"},
        {"code": "OTFRT", "name": "Outbound freight"},
        {"code": "OFFSP", "name": "Office supplies"},
        {"code": "WELF", "name": "Staff welfare"},
        {"code": "RPR", "name": "Repairs and maintenance"},
        {"code": "OTH", "name": "Other operating expense"}
      ]
      """
    And ExpenseType table should contain exactly these records:
      """json
      [
        {"code": "WGS", "name": "Wages"},
        {"code": "RNT", "name": "Rent", "expenseAccountCode": "005.003"},
        {"code": "UTIL", "name": "Utilities"},
        {"code": "INFRT", "name": "Inbound freight"},
        {"code": "OTFRT", "name": "Outbound freight"},
        {"code": "OFFSP", "name": "Office supplies"},
        {"code": "WELF", "name": "Staff welfare"},
        {"code": "RPR", "name": "Repairs and maintenance"},
        {"code": "OTH", "name": "Other operating expense"}
      ]
      """

  Scenario: Every seeded system expense type appears in the listing with its code and account
    When I GET to /secured/expense-types
    Then the response status should be 200
    And response contains details:
      """json
      [
        {"code": "WGS", "name": "Wages", "expenseAccountCode": "005.002"},
        {"code": "RNT", "name": "Rent", "expenseAccountCode": "005.003"},
        {"code": "UTIL", "name": "Utilities", "expenseAccountCode": "005.004"},
        {"code": "INFRT", "name": "Inbound freight", "expenseAccountCode": "005.005"},
        {"code": "OTFRT", "name": "Outbound freight", "expenseAccountCode": "005.006"},
        {"code": "OFFSP", "name": "Office supplies", "expenseAccountCode": "005.007"},
        {"code": "WELF", "name": "Staff welfare", "expenseAccountCode": "005.008"},
        {"code": "RPR", "name": "Repairs and maintenance", "expenseAccountCode": "005.010"},
        {"code": "OTH", "name": "Other operating expense", "expenseAccountCode": "005.013"}
      ]
      """

  Scenario: Listing by entry point keeps only the types that list it
    When I GET to /secured/expense-types with query parameters:
      | sourceType | WAGES |
    Then the response status should be 200
    And response contains details:
      """json
      [{"code": "WGS", "name": "Wages", "eligibleSourceTypes": ["WAGES"]}]
      """
    And response contains no item with details:
      """json
      {"code": "RNT"}
      """

  Scenario: A custom expense type is created with a trimmed name and its account
    When I POST to /secured/expense-types with payload:
      """json
      {
        "name": "  Fleet Fuel  ",
        "expenseAccountCode": "005.009",
        "eligiblePayeeTypes": ["SUPPLIER"],
        "eligibleSourceTypes": ["ADHOC"]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Fleet Fuel", "expenseAccountCode": "005.009"}
      """
    And ExpenseType table should contain exactly these records:
      """json
      [
        {"code": "WGS", "name": "Wages"},
        {"code": "RNT", "name": "Rent"},
        {"code": "UTIL", "name": "Utilities"},
        {"code": "INFRT", "name": "Inbound freight"},
        {"code": "OTFRT", "name": "Outbound freight"},
        {"code": "OFFSP", "name": "Office supplies"},
        {"code": "WELF", "name": "Staff welfare"},
        {"code": "RPR", "name": "Repairs and maintenance"},
        {"code": "OTH", "name": "Other operating expense"},
        {"name": "Fleet Fuel", "expenseAccountCode": "005.009"}
      ]
      """

  Scenario Outline: An expense type with a rule-breaking name or eligibility is rejected and nothing is saved
    When I POST to /secured/expense-types with payload:
      """json
      {
        "name": "<name>",
        "expenseAccountCode": "005.009",
        "eligiblePayeeTypes": <payeeTypes>,
        "eligibleSourceTypes": <sourceTypes>
      }
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And ExpenseType table should contain exactly these records:
      """json
      [
        {"code": "WGS", "name": "Wages"},
        {"code": "RNT", "name": "Rent"},
        {"code": "UTIL", "name": "Utilities"},
        {"code": "INFRT", "name": "Inbound freight"},
        {"code": "OTFRT", "name": "Outbound freight"},
        {"code": "OFFSP", "name": "Office supplies"},
        {"code": "WELF", "name": "Staff welfare"},
        {"code": "RPR", "name": "Repairs and maintenance"},
        {"code": "OTH", "name": "Other operating expense"}
      ]
      """

    Examples:
      | name            | payeeTypes   | sourceTypes | error                                                  |
      |                 | ["SUPPLIER"] | ["ADHOC"]   | Expense type name is required                          |
      | office SUPPLIES | ["SUPPLIER"] | ["ADHOC"]   | An expense type named 'office SUPPLIES' already exists |
      | Fleet Fuel      | []           | ["ADHOC"]   | At least one eligible payee type is required           |
      | Fleet Fuel      | ["SUPPLIER"] | []          | At least one eligible entry point is required          |

  Scenario Outline: An account that cannot back an expense type is rejected and nothing is saved
    When I POST to /secured/expense-types with payload:
      """json
      {
        "name": "Fleet Fuel",
        "expenseAccountCode": "<account>",
        "eligiblePayeeTypes": ["SUPPLIER"],
        "eligibleSourceTypes": ["ADHOC"]
      }
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And ExpenseType table should contain exactly these records:
      """json
      [
        {"code": "WGS", "name": "Wages"},
        {"code": "RNT", "name": "Rent"},
        {"code": "UTIL", "name": "Utilities"},
        {"code": "INFRT", "name": "Inbound freight"},
        {"code": "OTFRT", "name": "Outbound freight"},
        {"code": "OFFSP", "name": "Office supplies"},
        {"code": "WELF", "name": "Staff welfare"},
        {"code": "RPR", "name": "Repairs and maintenance"},
        {"code": "OTH", "name": "Other operating expense"}
      ]
      """

    Examples:
      | account     | error                                                                           |
      | 001.001.001 | Cash (1.1.1) is not an expense account                                          |
      | 009.999.999 | Expense account not found: 009.999.999                                          |
      | 005         | Expenses (5) has child accounts; Expense account must be a leaf account         |
      | 005.002     | Wages Expense (5.2) is reserved for system flows and cannot back an expense type |

  Scenario: The eligibility of a custom expense type is changed and its name kept
    Given a custom expense type named "Fleet Fuel" exists
    When I update the selected expense type with payload:
      """json
      {
        "id": "{{selectedExpenseType}}",
        "eligiblePayeeTypes": ["SUPPLIER", "OTHER"],
        "eligibleSourceTypes": ["ADHOC", "SALE"]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "id": "{{selectedExpenseType}}",
        "name": "Fleet Fuel",
        "eligiblePayeeTypes": ["SUPPLIER", "OTHER"],
        "eligibleSourceTypes": ["ADHOC", "SALE"]
      }
      """
    And ExpenseType should match example:
      """json
      {"name": "Fleet Fuel", "eligiblePayeeTypes": ["SUPPLIER", "OTHER"], "eligibleSourceTypes": ["ADHOC", "SALE"]}
      """

  Scenario: A system-defined expense type cannot be changed through the general update
    Given the expense type "Rent" is selected
    When I update the selected expense type with payload:
      """json
      {"id": "{{selectedExpenseType}}", "eligibleSourceTypes": ["ADHOC", "SALE"]}
      """
    Then the response status should be 400
    And the response error message should be "System-defined expense types can only be renamed; use the rename endpoint"
    And ExpenseType should not match example:
      """json
      {"name": "Rent", "eligibleSourceTypes": ["ADHOC", "SALE"]}
      """

  Scenario: An update that renames a type to an existing name is rejected
    Given a custom expense type named "Fleet Fuel" exists
    When I update the selected expense type with payload:
      """json
      {"id": "{{selectedExpenseType}}", "name": "rent"}
      """
    Then the response status should be 400
    And the response error message should be "An expense type named 'rent' already exists"
    And ExpenseType should match example:
      """json
      {"name": "Fleet Fuel"}
      """

  Scenario: An update that removes every payee type is rejected
    Given a custom expense type named "Fleet Fuel" exists
    When I update the selected expense type with payload:
      """json
      {"id": "{{selectedExpenseType}}", "eligiblePayeeTypes": []}
      """
    Then the response status should be 400
    And the response error message should be "At least one eligible payee type is required"
    And ExpenseType should match example:
      """json
      {"name": "Fleet Fuel", "eligiblePayeeTypes": ["SUPPLIER"]}
      """

  Scenario: An update for an expense type that does not exist is rejected
    When I PUT to /secured/expense-types with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000000", "name": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And ExpenseType table should contain exactly these records:
      """json
      [
        {"code": "WGS", "name": "Wages"},
        {"code": "RNT", "name": "Rent"},
        {"code": "UTIL", "name": "Utilities"},
        {"code": "INFRT", "name": "Inbound freight"},
        {"code": "OTFRT", "name": "Outbound freight"},
        {"code": "OFFSP", "name": "Office supplies"},
        {"code": "WELF", "name": "Staff welfare"},
        {"code": "RPR", "name": "Repairs and maintenance"},
        {"code": "OTH", "name": "Other operating expense"}
      ]
      """

  Scenario: A custom expense type is renamed with its surrounding spaces trimmed
    Given a custom expense type named "Fleet Fuel" exists
    When I rename the selected expense type to "  Fleet Costs  "
    Then the response status should be 200
    And response contains details:
      """json
      {"id": "{{selectedExpenseType}}", "name": "Fleet Costs"}
      """
    And ExpenseType should match example:
      """json
      {"name": "Fleet Costs"}
      """
    And ExpenseType should not match example:
      """json
      {"name": "Fleet Fuel"}
      """

  Scenario: A system-defined expense type is renamed and keeps its code and account
    Given the expense type "Rent" is selected
    When I rename the selected expense type to "Premises Rent"
    Then the response status should be 200
    And response contains details:
      """json
      {"id": "{{selectedExpenseType}}", "code": "RNT", "name": "Premises Rent", "expenseAccountCode": "005.003"}
      """
    And ExpenseType should match example:
      """json
      {"name": "Premises Rent", "code": "RNT"}
      """

  Scenario: A rename to a name held by another type, ignoring case, is rejected
    Given a custom expense type named "Fleet Fuel" exists
    When I rename the selected expense type to "wages"
    Then the response status should be 400
    And the response error message should be "An expense type named 'wages' already exists"
    And ExpenseType should match example:
      """json
      {"name": "Fleet Fuel"}
      """

  Scenario: A rename to a blank name is rejected
    Given a custom expense type named "Fleet Fuel" exists
    When I rename the selected expense type to "   "
    Then the response status should be 400
    And the response error message should be "Expense type name is required"
    And ExpenseType should match example:
      """json
      {"name": "Fleet Fuel"}
      """
