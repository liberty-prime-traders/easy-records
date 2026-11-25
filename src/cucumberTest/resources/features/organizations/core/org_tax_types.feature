Feature: Organization tax types
  As an organization admin
  I want to register the jurisdiction tax types my organization charges and recovers
  So that each registration points at accounts that match its recovery type

  Background:
    Given I am authenticated as a platform admin
    And the platform admin is an organization admin
    And tax configuration is active

  Scenario: An organization starts with no tax types registered
    When I GET to /secured/org-tax-types
    Then the response status should be 200
    And response list size is 0

  Scenario: Available lists a jurisdiction tax type the organization has not registered
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    When I GET to /secured/org-tax-types/available
    Then the response status should be 200
    And response contains details:
      """json
      [{"label": "Kenya", "children": [{"label": "Kenya - VAT", "selectable": true}]}]
      """

  Scenario: Registering a jurisdiction tax type removes it from available
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    And the organization has registered "Kenya - VAT" with payable "VAT Payable" and recoverable "VAT Recoverable"
    When I GET to /secured/org-tax-types/available
    Then the response status should be 200
    And response list size is 0
    And Org_Jurisdiction_Tax_Type table should contain exactly these records:
      """json
      [{"payableAccountCode": "002.002.001", "recoverableAccountCode": "001.001.004.001"}]
      """

  Scenario: Creating a tax type stores it and shows its account names
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 200
    And response contains details:
      """json
      [
        {
          "taxLabel": "Kenya - VAT",
          "payableAccount": "VAT Payable (2.2.1)",
          "recoverableAccount": "VAT Recoverable (1.1.4.1)"
        }
      ]
      """
    When I GET to /secured/org-tax-types
    Then response list size is 1
    And response contains details:
      """json
      [
        {
          "taxLabel": "Kenya - VAT",
          "payableAccount": "VAT Payable (2.2.1)",
          "recoverableAccount": "VAT Recoverable (1.1.4.1)"
        }
      ]
      """
    And Org_Jurisdiction_Tax_Type should match example:
      """json
      {"payableAccountCode": "002.002.001", "recoverableAccountCode": "001.001.004.001"}
      """

  Scenario: A non-recoverable tax type is registered with only a payable account
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "UK" exists
    And a non-recoverable tax type named "Levy" exists
    And the tax type "Levy" applies in the jurisdiction "UK"
    And a payable tax account named "VAT Payable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:UK - Levy}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}"
        }
      ]
      """
    Then the response status should be 200
    And response contains details:
      """json
      [
        {
          "taxLabel": "UK - Levy",
          "taxRecoveryType": "NON_RECOVERABLE",
          "payableAccount": "VAT Payable (2.2.1)",
          "recoverableAccount": null
        }
      ]
      """
    And Org_Jurisdiction_Tax_Type should match example:
      """json
      {"payableAccountCode": "002.002.001", "recoverableAccountCode": null}
      """

  Scenario: Several jurisdiction tax types are registered in one request
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a jurisdiction named "UK" exists
    And a tax type named "VAT" exists
    And a non-recoverable tax type named "Levy" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And the tax type "Levy" applies in the jurisdiction "UK"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        },
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:UK - Levy}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}"
        }
      ]
      """
    Then the response status should be 200
    And response list size is 2
    And response contains details:
      """json
      [
        {
          "taxLabel": "Kenya - VAT",
          "payableAccount": "VAT Payable (2.2.1)",
          "recoverableAccount": "VAT Recoverable (1.1.4.1)"
        },
        {"taxLabel": "UK - Levy", "payableAccount": "VAT Payable (2.2.1)", "recoverableAccount": null}
      ]
      """
    And Org_Jurisdiction_Tax_Type table should contain exactly these records:
      """json
      [
        {"payableAccountCode": "002.002.001", "recoverableAccountCode": "001.001.004.001"},
        {"payableAccountCode": "002.002.001", "recoverableAccountCode": null}
      ]
      """

  Scenario: A payable account must exist
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "999.999",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Payable tax account not found: 999.999"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: A payable account must be a liability
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Recoverable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Org-defined payable tax account must be of type Liability"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: The system Tax Payable account cannot be used as a payable account
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "002.002",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Payable tax account must be under the TAX_PAYABLE parent account"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: A recoverable tax type requires a recoverable account
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": null
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Kenya - VAT requires a recoverable account"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: A recoverable account must be an asset
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Payable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Org-defined recoverable tax account must be of type Asset"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: A payable account is required
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Every tax type must be associated with a payable account"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: A non-recoverable tax type rejects a recoverable account, so a batch saves nothing
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a jurisdiction named "UK" exists
    And a tax type named "VAT" exists
    And a non-recoverable tax type named "Levy" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And the tax type "Levy" applies in the jurisdiction "UK"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        },
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:UK - Levy}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "UK - Levy is not recoverable, so a recoverable account should not be provided"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: The same jurisdiction tax type cannot appear twice in one request
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        },
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Duplicate jurisdiction tax type in request: Kenya - VAT"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: A jurisdiction tax type already registered cannot be registered again
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    And the organization has registered "Kenya - VAT" with payable "VAT Payable" and recoverable "VAT Recoverable"
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Kenya - VAT is already registered for this organization"
    And Org_Jurisdiction_Tax_Type table should contain exactly these records:
      """json
      [{"payableAccountCode": "002.002.001", "recoverableAccountCode": "001.001.004.001"}]
      """

  Scenario: A jurisdiction tax type that does not exist is rejected
    Given a payable tax account named "VAT Payable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "00000000-0000-0000-0000-000000000001",
          "payableAccountCode": "{{taxAccount:VAT Payable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Jurisdiction tax type not found: 00000000-0000-0000-0000-000000000001"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: A jurisdiction tax type stopped on its jurisdiction cannot be registered
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And the tax type "VAT" is stopped in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    When I POST the organization tax path /secured/org-tax-types with payload:
      """json
      [
        {
          "jurisdictionTaxTypeId": "{{jurisdictionTaxType:Kenya - VAT}}",
          "payableAccountCode": "{{taxAccount:VAT Payable}}",
          "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"
        }
      ]
      """
    Then the response status should be 400
    And the response error message should be "Jurisdiction tax type not found or is already stopped: Kenya - VAT"
    And Org_Jurisdiction_Tax_Type table should have no records in database

  Scenario: Changing a registration's status to inactive keeps the registration
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    And the organization has registered "Kenya - VAT" with payable "VAT Payable" and recoverable "VAT Recoverable"
    When I PUT the organization tax path /secured/org-tax-types with payload:
      """json
      {"id": "{{orgTaxType:Kenya - VAT}}", "status": "INACTIVE"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"taxLabel": "Kenya - VAT", "status": "INACTIVE"}
      """
    And Org_Jurisdiction_Tax_Type should match example:
      """json
      {"status": "INACTIVE", "recoverableAccountCode": "001.001.004.001"}
      """

  Scenario: Changing a registration's payable account moves it to the new account
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    And the organization has registered "Kenya - VAT" with payable "VAT Payable" and recoverable "VAT Recoverable"
    And a payable tax account named "VAT Interest Payable" exists
    When I PUT the organization tax path /secured/org-tax-types with payload:
      """json
      {"id": "{{orgTaxType:Kenya - VAT}}", "payableAccountCode": "{{taxAccount:VAT Interest Payable}}"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"taxLabel": "Kenya - VAT", "payableAccountCode": "002.002.002", "payableAccount": "VAT Interest Payable (2.2.2)"}
      """
    And Org_Jurisdiction_Tax_Type should match example:
      """json
      {"payableAccountCode": "002.002.002"}
      """

  Scenario: Clearing the recoverable account of a recoverable tax type is rejected
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    And the organization has registered "Kenya - VAT" with payable "VAT Payable" and recoverable "VAT Recoverable"
    When I PUT the organization tax path /secured/org-tax-types with payload:
      """json
      {"id": "{{orgTaxType:Kenya - VAT}}", "recoverableAccountCode": ""}
      """
    Then the response status should be 400
    And the response error message should be "Kenya - VAT requires a recoverable account"
    And Org_Jurisdiction_Tax_Type should match example:
      """json
      {"recoverableAccountCode": "001.001.004.001"}
      """

  Scenario: Updating a non-recoverable registration drops the recoverable account it is given
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "UK" exists
    And a non-recoverable tax type named "Levy" exists
    And the tax type "Levy" applies in the jurisdiction "UK"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    And the organization has registered "UK - Levy" with payable "VAT Payable"
    When I PUT the organization tax path /secured/org-tax-types with payload:
      """json
      {"id": "{{orgTaxType:UK - Levy}}", "recoverableAccountCode": "{{taxAccount:VAT Recoverable}}"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"taxLabel": "UK - Levy", "payableAccount": "VAT Payable (2.2.1)", "recoverableAccount": null}
      """
    And Org_Jurisdiction_Tax_Type should match example:
      """json
      {"recoverableAccountCode": null}
      """

  Scenario: Updating a registration that does not exist is rejected
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a tax type named "VAT" exists
    And the tax type "VAT" applies in the jurisdiction "Kenya"
    And a payable tax account named "VAT Payable" exists
    And a recoverable tax account named "VAT Recoverable" exists
    And the organization has registered "Kenya - VAT" with payable "VAT Payable" and recoverable "VAT Recoverable"
    When I PUT the organization tax path /secured/org-tax-types with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "status": "INACTIVE"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Org_Jurisdiction_Tax_Type table should contain exactly these records:
      """json
      [{"status": "ACTIVE"}]
      """
