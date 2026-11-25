Feature: Accounts
  As an authenticated organization user
  I want to maintain the chart of accounts
  So that every account has a unique code, a balance and the right place in the tree

  Scenario: The chart lists the seeded accounts with a zero balance
    When I GET to /secured/accounts
    Then the response status should be 200
    And response contains details:
      """json
      [
        {"code": "001", "name": "Assets", "currentBalance": 0},
        {"code": "001.001", "name": "Current Assets", "currentBalance": 0},
        {"code": "001.001.001", "name": "Cash", "currentBalance": 0},
        {"code": "001.001.002", "name": "Accounts Receivable", "currentBalance": 0},
        {"code": "001.001.002.001", "name": "Trade Receivables", "currentBalance": 0},
        {"code": "001.001.002.002", "name": "Allowance for Doubtful Accounts", "currentBalance": 0},
        {"code": "001.001.003", "name": "Inventory", "currentBalance": 0},
        {"code": "001.001.004", "name": "Tax Recoverable", "currentBalance": 0},
        {"code": "001.001.005", "name": "Digital Payments", "currentBalance": 0},
        {"code": "001.001.006", "name": "Inventory in Transit", "currentBalance": 0},
        {"code": "001.002", "name": "Fixed Assets", "currentBalance": 0},
        {"code": "001.002.001", "name": "Furniture & Fixtures", "currentBalance": 0},
        {"code": "001.002.002", "name": "Equipment", "currentBalance": 0},
        {"code": "001.002.003", "name": "Vehicles", "currentBalance": 0},
        {"code": "001.002.004", "name": "Buildings", "currentBalance": 0},
        {"code": "001.002.005", "name": "Land", "currentBalance": 0},
        {"code": "001.002.006", "name": "Accumulated Depreciation", "currentBalance": 0},
        {"code": "002", "name": "Liabilities", "currentBalance": 0},
        {"code": "002.001", "name": "Accounts Payable", "currentBalance": 0},
        {"code": "002.001.001", "name": "Trade Payables", "currentBalance": 0},
        {"code": "002.001.002", "name": "Purchase Discounts", "currentBalance": 0},
        {"code": "002.002", "name": "Tax Payable", "currentBalance": 0},
        {"code": "002.003", "name": "Wages Payable", "currentBalance": 0},
        {"code": "002.004", "name": "Loans Payable", "currentBalance": 0},
        {"code": "003", "name": "Equity", "currentBalance": 0},
        {"code": "003.001", "name": "Owner's Capital", "currentBalance": 0},
        {"code": "003.002", "name": "Retained Earnings", "currentBalance": 0},
        {"code": "003.003", "name": "Owner's Draws", "currentBalance": 0},
        {"code": "003.004", "name": "Opening Balance Equity", "currentBalance": 0},
        {"code": "004", "name": "Revenue", "currentBalance": 0},
        {"code": "004.001", "name": "Sales Revenue", "currentBalance": 0},
        {"code": "004.001.001", "name": "Gross Sales", "currentBalance": 0},
        {"code": "004.001.002", "name": "Sales Returns", "currentBalance": 0},
        {"code": "004.001.003", "name": "Sales Allowances", "currentBalance": 0},
        {"code": "004.001.004", "name": "Sales Discounts", "currentBalance": 0},
        {"code": "004.002", "name": "Other Income", "currentBalance": 0},
        {"code": "005", "name": "Expenses", "currentBalance": 0},
        {"code": "005.001", "name": "Cost of Goods", "currentBalance": 0},
        {"code": "005.001.001", "name": "Cost of Goods Sold", "currentBalance": 0},
        {"code": "005.001.002", "name": "Free Goods Received", "currentBalance": 0},
        {"code": "005.002", "name": "Wages Expense", "currentBalance": 0},
        {"code": "005.003", "name": "Rent Expense", "currentBalance": 0},
        {"code": "005.004", "name": "Utilities Expense", "currentBalance": 0},
        {"code": "005.005", "name": "Inbound Freight", "currentBalance": 0},
        {"code": "005.006", "name": "Outbound Freight", "currentBalance": 0},
        {"code": "005.007", "name": "Office Supplies", "currentBalance": 0},
        {"code": "005.008", "name": "Staff Welfare", "currentBalance": 0},
        {"code": "005.009", "name": "Equipment & Electronics", "currentBalance": 0},
        {"code": "005.010", "name": "Repairs & Maintenance", "currentBalance": 0},
        {"code": "005.011", "name": "Shrinkage & Losses", "currentBalance": 0},
        {"code": "005.012", "name": "Bad Debt Expense", "currentBalance": 0},
        {"code": "005.013", "name": "Other Operating Expenses", "currentBalance": 0},
        {"code": "005.014", "name": "Tax Expense", "currentBalance": 0},
        {"code": "005.015", "name": "Internal Use Expense", "currentBalance": 0}
      ]
      """
    And account "001.001.001" should have a balance of "0"

  Scenario: The chart lists every seeded account
    When I GET to /secured/accounts
    Then the response status should be 200
    And response list size is 54
    And Account table should have exactly 54 records in database

  Scenario: A new root account is stored under code 100 with a zero balance
    When I POST to /secured/accounts/root with payload:
      """json
      {"name": "Equipment Loans", "accountType": "LIABILITY"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "100", "name": "Equipment Loans", "currentBalance": 0}
      """
    And Account should match example:
      """json
      {"code": "100"}
      """
    And account "100" should have a balance of "0"

  Scenario: A second root account takes the code after the first
    Given a root account named "Equipment Loans" of type "LIABILITY" exists
    When I POST to /secured/accounts/root with payload:
      """json
      {"name": "Vehicle Loans", "accountType": "LIABILITY"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "101", "name": "Vehicle Loans"}
      """
    And Account should match example:
      """json
      {"code": "101"}
      """

  Scenario: A contra type cannot be a root account
    When I POST to /secured/accounts/root with payload:
      """json
      {"name": "Discounts", "accountType": "ASSET_CONTRA"}
      """
    Then the response status should be 400
    And the response error message should be "Only certain account types can be created as root accounts and ASSET_CONTRA is not one of them"
    And Account should not match example:
      """json
      {"name": "Discounts"}
      """
    When I GET to /secured/accounts
    Then response contains no item with details:
      """json
      {"name": "Discounts"}
      """

  Scenario: A root account name already in use is rejected and nothing is stored
    Given a root account named "Equipment Loans" of type "LIABILITY" exists
    When I POST to /secured/accounts/root with payload:
      """json
      {"name": "Equipment Loans", "accountType": "ASSET"}
      """
    Then the response status should be 400
    And the response error message should be "A root account with the same name already exists"
    And Account should not match example:
      """json
      {"code": "101"}
      """
    When I GET to /secured/accounts
    Then response contains no item with details:
      """json
      {"code": "101"}
      """

  Scenario: A user account can be renamed and the new name is stored
    Given a root account named "Equipment Loans" of type "LIABILITY" exists
    When I rename the account with code "100" to "Equipment Financing"
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "100", "name": "Equipment Financing"}
      """
    And Account should match example:
      """json
      {"code": "100", "name": "Equipment Financing"}
      """

  Scenario: A system account cannot be renamed
    When I rename the account with code "001.001.001" to "Petty Cash"
    Then the response status should be 400
    And the response error message should be "System accounts cannot be renamed"
    And Account should match example:
      """json
      {"code": "001.001.001", "name": "Cash"}
      """
    When I GET to /secured/accounts
    Then response contains item with details:
      """json
      {"code": "001.001.001", "name": "Cash"}
      """

  Scenario: A system account cannot be deactivated
    When I deactivate the account with code "001.001.001"
    Then the response status should be 400
    And the response error message should be "System accounts cannot be activated or deactivated"
    And Account should match example:
      """json
      {"code": "001.001.001", "name": "Cash", "accountIsActive": true}
      """
    When I GET to /secured/accounts
    Then response contains item with details:
      """json
      {"code": "001.001.001", "name": "Cash", "accountIsActive": true}
      """

  Scenario: A deactivated account is stored as inactive
    Given a root account named "Equipment Loans" of type "LIABILITY" exists
    When I deactivate the account with code "100"
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "100", "name": "Equipment Loans", "accountIsActive": false}
      """
    And Account should match example:
      """json
      {"code": "100", "accountIsActive": false}
      """

  Scenario: A reactivated account accepts children again
    Given a root account named "Equipment Loans" of type "LIABILITY" exists
    And I deactivate the account with code "100"
    When I activate the account with code "100"
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "100", "name": "Equipment Loans", "accountIsActive": true}
      """
    And Account should match example:
      """json
      {"code": "100", "accountIsActive": true}
      """
    When I POST to /secured/accounts/child with payload:
      """json
      {"parentAccountCode": "100", "name": "Bank Loans"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "100.001", "name": "Bank Loans", "parentAccountCode": "100"}
      """
    And Account should match example:
      """json
      {"code": "100.001", "name": "Bank Loans", "parentAccountCode": "100"}
      """

  Scenario: A child added under Digital Payments takes the next code beneath it
    When I POST to /secured/accounts/child with payload:
      """json
      {"parentAccountCode": "001.001.005", "name": "Bank"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "001.001.005.001", "name": "Bank", "parentAccountCode": "001.001.005"}
      """
    And Account should match example:
      """json
      {"code": "001.001.005.001", "name": "Bank", "parentAccountCode": "001.001.005"}
      """
    And account "001.001.005.001" should have a balance of "0"
    And Account should match example:
      """json
      {"code": "001.001.005", "name": "Digital Payments"}
      """
