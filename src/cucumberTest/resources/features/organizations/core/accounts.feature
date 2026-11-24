Feature: Accounts
  As an authenticated organization user
  I want to maintain the chart of accounts
  So that every account has a unique code, a balance and the right place in the tree

  Scenario: The chart lists the seeded accounts with a zero balance
    When I GET to /secured/accounts
    Then the response status should be 200
    And response contains item with details:
      """json
      {"code": "001.001.001", "name": "Cash", "accountType": "ASSET",
       "accountIsActive": true, "accountIsSystemMaintained": true}
      """
    And account "001.001.001" should have a balance of "0"

  Scenario: A new root account is stored under code 100 with a zero balance
    When I POST to /secured/accounts/root with payload:
      """json
      {"name": "Equipment Loans", "accountType": "LIABILITY"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "100", "name": "Equipment Loans", "accountType": "LIABILITY",
       "accountIsActive": true, "accountIsSystemMaintained": false}
      """
    And Account should match example:
      """json
      {"code": "100", "name": "Equipment Loans", "accountType": "LIABILITY", "accountIsActive": true}
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
      {"code": "101", "name": "Vehicle Loans"}
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
      {"code": "001.001.001", "accountIsActive": true}
      """
    When I GET to /secured/accounts
    Then response contains item with details:
      """json
      {"code": "001.001.001", "accountIsActive": true}
      """

  Scenario: A deactivated account is stored as inactive
    Given a root account named "Equipment Loans" of type "LIABILITY" exists
    When I deactivate the account with code "100"
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "100", "accountIsActive": false}
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
      {"code": "100", "accountIsActive": true}
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
      {"code": "100.001", "name": "Bank Loans", "accountType": "LIABILITY",
       "parentAccountCode": "100"}
      """

  Scenario: A child added under Digital Payments takes the next code beneath it
    When I POST to /secured/accounts/child with payload:
      """json
      {"parentAccountCode": "001.001.005", "name": "Bank"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"code": "001.001.005.001", "name": "Bank", "accountType": "ASSET",
       "parentAccountCode": "001.001.005", "accountIsExtensible": false}
      """
    And Account should match example:
      """json
      {"code": "001.001.005.001", "name": "Bank", "parentAccountCode": "001.001.005",
       "accountIsSystemMaintained": false}
      """
    And account "001.001.005.001" should have a balance of "0"
