Feature: Organization features and accounting configuration
  As an organization user
  I want to activate and deactivate features and read or change the accounting configuration they gate
  So that accounting endpoints answer only while Chart of Accounts is active

  Scenario: Chart of Accounts is active and Tax Configuration is not when an organization starts
    When I GET to /secured/org-features
    Then response returns with status 200
    And response contains details:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "ACTIVE"}]
      """
    And Organization_Feature table should contain exactly these records:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "ACTIVE"}]
      """

  Scenario: Activating Tax Configuration stores an active feature row
    When I POST to /secured/org-features/activate with payload:
      """json
      ["TAX_CONFIGURATION"]
      """
    Then response returns with status 200
    And response contains details:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "ACTIVE"}, {"feature": "TAX_CONFIGURATION", "status": "ACTIVE"}]
      """
    And Organization_Feature should match example:
      """json
      {"feature": "TAX_CONFIGURATION", "status": "ACTIVE"}
      """
    And Organization_Feature table should have exactly 2 records in database

  Scenario: Activating Tax Configuration is refused while Chart of Accounts is inactive
    When I POST to /secured/org-features/deactivate with payload:
      """json
      ["CHART_OF_ACCOUNTS"]
      """
    Then response returns with status 200
    When I POST to /secured/org-features/activate with payload:
      """json
      ["TAX_CONFIGURATION"]
      """
    Then response returns with status 400
    And the response error message should be "Cannot activate feature TAX_CONFIGURATION"
    And the response should match json:
      """json
      {"body": ["'Tax Configuration' cannot be activated unless 'Chart of Accounts' is active"]}
      """
    And Organization_Feature should not match example:
      """json
      {"feature": "TAX_CONFIGURATION"}
      """
    And Organization_Feature table should contain exactly these records:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "INACTIVE"}]
      """

  Scenario: Deactivating Chart of Accounts makes the accounting configuration endpoint answer 400
    When I POST to /secured/org-features/deactivate with payload:
      """json
      ["CHART_OF_ACCOUNTS"]
      """
    Then response returns with status 200
    And response contains details:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "INACTIVE"}]
      """
    And Organization_Feature should match example:
      """json
      {"feature": "CHART_OF_ACCOUNTS", "status": "INACTIVE"}
      """
    When I GET to /secured/accounting-config
    Then response returns with status 400
    And the response error message should be "Feature CHART_OF_ACCOUNTS is not enabled for this organization"

  Scenario: Deactivating a feature that was never activated adds no feature row
    When I POST to /secured/org-features/deactivate with payload:
      """json
      ["TAX_CONFIGURATION"]
      """
    Then response returns with status 200
    And response contains details:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "ACTIVE"}]
      """
    And Organization_Feature table should contain exactly these records:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "ACTIVE"}]
      """

  Scenario: Reactivating Chart of Accounts keeps the stored accounting configuration
    When I PUT to /secured/accounting-config with payload:
      """json
      {"periodPrepDays": 5}
      """
    Then response returns with status 200
    When I POST to /secured/org-features/deactivate with payload:
      """json
      ["CHART_OF_ACCOUNTS"]
      """
    Then response returns with status 200
    When I POST to /secured/org-features/activate with payload:
      """json
      ["CHART_OF_ACCOUNTS"]
      """
    Then response returns with status 200
    And response contains details:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "ACTIVE"}]
      """
    When I GET to /secured/accounting-config
    Then response returns with status 200
    And response contains details:
      """json
      {"periodPrepDays": 5}
      """
    And Org_Accounting_Config table should contain exactly these records:
      """json
      [{"periodPrepDays": 5}]
      """

  Scenario: Activating an already active Chart of Accounts keeps a single accounting configuration
    When I POST to /secured/org-features/activate with payload:
      """json
      ["CHART_OF_ACCOUNTS"]
      """
    Then response returns with status 200
    And response contains details:
      """json
      [{"feature": "CHART_OF_ACCOUNTS", "status": "ACTIVE"}]
      """
    And Org_Accounting_Config table should have exactly 1 records in database

  Scenario: A new organization starts with monthly periods, Monday week starts and two prep days
    When I GET to /secured/accounting-config
    Then response returns with status 200
    And response contains details:
      """json
      {"fiscalPeriodCycle": "MONTHLY", "periodWeekStartDay": "MONDAY", "periodPrepDays": 2}
      """
    And Org_Accounting_Config should match example:
      """json
      {"fiscalPeriodCycle": "MONTHLY", "periodWeekStartDay": "MONDAY", "periodPrepDays": 2}
      """

  Scenario: Updating only the fiscal period cycle keeps the week start day and prep days
    When I PUT to /secured/accounting-config with payload:
      """json
      {"fiscalPeriodCycle": "QUARTERLY"}
      """
    Then response returns with status 200
    And response contains details:
      """json
      {"fiscalPeriodCycle": "QUARTERLY", "periodWeekStartDay": "MONDAY", "periodPrepDays": 2}
      """
    And Org_Accounting_Config should match example:
      """json
      {"fiscalPeriodCycle": "QUARTERLY", "periodWeekStartDay": "MONDAY", "periodPrepDays": 2}
      """
    When I GET to /secured/accounting-config
    Then response returns with status 200
    And response contains details:
      """json
      {"fiscalPeriodCycle": "QUARTERLY", "periodWeekStartDay": "MONDAY", "periodPrepDays": 2}
      """

  Scenario: Updating the week start day and prep days stores both values
    When I PUT to /secured/accounting-config with payload:
      """json
      {"periodWeekStartDay": "SUNDAY", "periodPrepDays": 3}
      """
    Then response returns with status 200
    And response contains details:
      """json
      {"periodWeekStartDay": "SUNDAY", "periodPrepDays": 3}
      """
    And Org_Accounting_Config should match example:
      """json
      {"fiscalPeriodCycle": "MONTHLY", "periodWeekStartDay": "SUNDAY", "periodPrepDays": 3}
      """

  Scenario: Updating the accounting configuration is refused while Chart of Accounts is inactive
    When I POST to /secured/org-features/deactivate with payload:
      """json
      ["CHART_OF_ACCOUNTS"]
      """
    Then response returns with status 200
    When I PUT to /secured/accounting-config with payload:
      """json
      {"periodPrepDays": 7}
      """
    Then response returns with status 400
    And the response error message should be "Feature CHART_OF_ACCOUNTS is not enabled for this organization"
    And Org_Accounting_Config should match example:
      """json
      {"periodPrepDays": 2}
      """

  Scenario: An unknown feature code is rejected and no feature row is written
    When I POST to /secured/org-features/activate with payload:
      """json
      ["UNKNOWN_FEATURE"]
      """
    Then response returns with status 400
    And the response error message should be "The request body could not be read"
    And Organization_Feature table should have exactly 1 records in database
