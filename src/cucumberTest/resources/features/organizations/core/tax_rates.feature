Feature: Tax rates
  As an organization admin
  I want to set the rates a registered tax type charges over time
  So that each sale is taxed at the rate in force on its date

  Background:
    Given I am authenticated as a platform admin
    And the platform admin is an organization admin
    And tax configuration is active
    And a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    And the organization has registered "Kenya - VAT" with payable "VAT Payable" and recoverable "VAT Recoverable"

  Scenario: An organization starts with no tax rates
    When I GET to /secured/tax-rates
    Then the response status should be 200
    And response list size is 0

  Scenario: A percentage rate is created under a registered tax type
    When I POST the tax rate path /secured/tax-rates with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"taxLabel": "Kenya - VAT", "name": "Standard", "ratePercentage": 16}
      """
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Standard", "ratePercentage": 16}]
      """

  Scenario: A listed rate shows its tax label and that its registration is active
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    When I GET to /secured/tax-rates
    Then the response status should be 200
    And response list size is 1
    And response contains details:
      """json
      [{"taxLabel": "Kenya - VAT", "name": "Standard", "parentIsActive": true}]
      """
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Standard"}]
      """

  Scenario: A rate reports its registration as inactive once the registration is stopped
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    When I PUT the organization tax path /secured/org-tax-types with payload:
      """json
      {"id": "{{orgTaxType:Kenya - VAT}}", "status": "INACTIVE"}
      """
    And I GET to /secured/tax-rates
    Then the response status should be 200
    And response contains details:
      """json
      [{"name": "Standard", "parentIsActive": false}]
      """
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Standard"}]
      """

  Scenario: A rate cannot be created under a registration that is inactive
    When I PUT the organization tax path /secured/org-tax-types with payload:
      """json
      {"id": "{{orgTaxType:Kenya - VAT}}", "status": "INACTIVE"}
      """
    And I POST the tax rate path /secured/tax-rates with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    Then the response status should be 400
    And the response error message should be "Cannot create a rate for an inactive Tax Type"
    And Tax_Rate table should have no records in database

  Scenario: A fixed value tax type requires a flat amount
    Given a fixed value tax type named "Stamp Duty" exists
    And the tax type "Stamp Duty" applies in the jurisdiction "Kenya"
    And the organization has registered "Kenya - Stamp Duty" with payable "VAT Payable"
    When I POST the tax rate path /secured/tax-rates with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - Stamp Duty}}",
        "name": "Stamp",
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    Then the response status should be 400
    And the response error message should be "rateFlatAmount is required for FIXED_VALUE tax types"
    And Tax_Rate table should have no records in database

  Scenario: A fixed value rate stores its flat amount
    Given a fixed value tax type named "Stamp Duty" exists
    And the tax type "Stamp Duty" applies in the jurisdiction "Kenya"
    And the organization has registered "Kenya - Stamp Duty" with payable "VAT Payable"
    When I POST the tax rate path /secured/tax-rates with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - Stamp Duty}}",
        "name": "Stamp",
        "rateFlatAmount": 5.5,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Stamp", "rateFlatAmount": 5.5}
      """
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Stamp", "rateFlatAmount": 5.5}]
      """

  Scenario Outline: Line item rates with the same name cannot overlap
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    When I POST the tax rate path /secured/tax-rates with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 18,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-06-01"
      }
      """
    Then the response status should be 400
    And the response error message should be "<overlapMessage>"
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Standard", "startDate": "2030-01-01"}]
      """

    Examples:
      | overlapMessage                                                                                                  |
      | 'Standard' starts on 2030-01-01 and never expires. Please adjust your dates to avoid this overlap. |

  Scenario: Line item rates with different names may overlap
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    When I POST the tax rate path /secured/tax-rates with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Reduced",
        "ratePercentage": 5,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-06-01"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Reduced", "startDate": "2030-06-01"}
      """
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Standard", "startDate": "2030-01-01"}, {"name": "Reduced", "startDate": "2030-06-01"}]
      """

  Scenario: Rates that follow each other without overlap are accepted
    Given the tax rate "Standard 2030" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01",
        "endDate": "2030-12-31"
      }
      """
    When I POST the tax rate path /secured/tax-rates with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 18,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2031-01-01"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Standard", "ratePercentage": 18, "startDate": "2031-01-01"}
      """
    And Tax_Rate table should contain exactly these records:
      """json
      [
        {"name": "Standard", "startDate": "2030-01-01", "endDate": "2030-12-31"},
        {"name": "Standard", "startDate": "2031-01-01"}
      ]
      """

  Scenario: Renaming a rate keeps its dates
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    When I PUT the tax rate path /secured/tax-rates with payload:
      """json
      {"id": "{{taxRate:Standard}}", "name": "Reduced"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Reduced", "startDate": "2030-01-01", "endDate": null}
      """
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Reduced", "startDate": "2030-01-01"}]
      """

  Scenario: Setting an end date on a rate
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    When I PUT the tax rate path /secured/tax-rates with payload:
      """json
      {"id": "{{taxRate:Standard}}", "endDate": "2031-12-31"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Standard", "endDate": "2031-12-31"}
      """
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Standard", "endDate": "2031-12-31"}]
      """

  Scenario Outline: A rate renamed to the name of a rate it overlaps is rejected
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01",
        "endDate": "2030-12-31"
      }
      """
    And the tax rate "Reduced" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Reduced",
        "ratePercentage": 5,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-06-01"
      }
      """
    When I PUT the tax rate path /secured/tax-rates with payload:
      """json
      {"id": "{{taxRate:Standard}}", "name": "Reduced"}
      """
    Then the response status should be 400
    And the response error message should be "<overlapMessage>"
    And Tax_Rate table should contain exactly these records:
      """json
      [
        {"name": "Standard", "startDate": "2030-01-01", "endDate": "2030-12-31"},
        {"name": "Reduced", "startDate": "2030-06-01"}
      ]
      """

    Examples:
      | overlapMessage |
      | 'Reduced' starts on 2030-06-01 and never expires. Please adjust your dates to avoid this overlap. |

  Scenario: An update that changes nothing is rejected
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    When I PUT the tax rate path /secured/tax-rates with payload:
      """json
      {"id": "{{taxRate:Standard}}"}
      """
    Then the response status should be 400
    And the response error message should be "Nothing to update"
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Standard"}]
      """

  Scenario: Updating a rate that does not exist is rejected
    Given the tax rate "Standard" is created with payload:
      """json
      {
        "orgJurisdictionTaxTypeId": "{{orgTaxType:Kenya - VAT}}",
        "name": "Standard",
        "ratePercentage": 16,
        "taxIsBilledToCustomerSeparately": false,
        "taxIsIncludedInTaxableAmount": false,
        "startDate": "2030-01-01"
      }
      """
    When I PUT the tax rate path /secured/tax-rates with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "name": "Reduced"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Tax_Rate table should contain exactly these records:
      """json
      [{"name": "Standard"}]
      """
