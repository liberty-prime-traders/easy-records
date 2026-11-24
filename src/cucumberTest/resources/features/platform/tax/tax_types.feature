Feature: Tax types
  As a platform admin
  I want to define the taxes the platform can apply
  So that every tax has a unique name, at least one trigger and a clear recovery and application level

  Background:
    Given I am authenticated as a platform admin

  Scenario: Creating a tax type stores it with its settings and lists it
    When I POST to /secured/tax-types with payload:
      """json
      {
        "name": "GST",
        "description": "Goods and services tax",
        "calculationMethod": "PERCENTAGE",
        "taxRecoveryType": "RECOVERABLE",
        "taxApplicationLevel": "LINE_ITEM",
        "taxTriggers": ["SALE", "PURCHASE_RETURN"]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "name": "GST",
        "description": "Goods and services tax",
        "calculationMethod": "PERCENTAGE",
        "taxRecoveryType": "RECOVERABLE",
        "taxApplicationLevel": "LINE_ITEM",
        "taxTriggers": ["SALE", "PURCHASE_RETURN"]
      }
      """
    When I GET to /secured/tax-types
    Then response contains item with details:
      """json
      {"name": "GST"}
      """
    And Tax_Type should match example:
      """json
      {
        "name": "GST",
        "description": "Goods and services tax",
        "calculationMethod": "PERCENTAGE",
        "taxRecoveryType": "RECOVERABLE",
        "taxApplicationLevel": "LINE_ITEM",
        "taxTriggers": ["SALE", "PURCHASE_RETURN"]
      }
      """

  Scenario: A tax type needs a name
    When I POST to /secured/tax-types with payload:
      """json
      {
        "name": "   ",
        "calculationMethod": "PERCENTAGE",
        "taxRecoveryType": "RECOVERABLE",
        "taxApplicationLevel": "LINE_ITEM",
        "taxTriggers": ["SALE"]
      }
      """
    Then the response status should be 400
    And the response error message should be "Name must not be blank"
    And Tax_Type table should have no records in database

  Scenario: A tax type needs at least one trigger
    When I POST to /secured/tax-types with payload:
      """json
      {
        "name": "GST",
        "calculationMethod": "PERCENTAGE",
        "taxRecoveryType": "RECOVERABLE",
        "taxApplicationLevel": "LINE_ITEM",
        "taxTriggers": []
      }
      """
    Then the response status should be 400
    And the response error message should be "At least one tax trigger is required"
    And Tax_Type table should have no records in database

  Scenario Outline: Tax type names are unique ignoring case and spacing
    Given a tax type named "GST" exists
    When I POST to /secured/tax-types with payload:
      """json
      {
        "name": "<name>",
        "calculationMethod": "PERCENTAGE",
        "taxRecoveryType": "RECOVERABLE",
        "taxApplicationLevel": "LINE_ITEM",
        "taxTriggers": ["SALE"]
      }
      """
    Then the response status should be 400
    And the response error message should be "A tax type named '<name>' already exists"
    And Tax_Type table should have exactly 1 records in database

    Examples:
      | name  |
      | gst   |
      | G S T |

  Scenario: Renaming a tax type frees its old name
    Given a tax type named "GST" exists
    When I PUT the platform path /secured/tax-types with payload:
      """json
      {"id": "{{taxType:GST}}", "name": "VAT"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "VAT"}
      """
    And Tax_Type should match example:
      """json
      {"name": "VAT"}
      """
    When I POST to /secured/tax-types with payload:
      """json
      {
        "name": "GST",
        "calculationMethod": "PERCENTAGE",
        "taxRecoveryType": "RECOVERABLE",
        "taxApplicationLevel": "LINE_ITEM",
        "taxTriggers": ["SALE"]
      }
      """
    Then the response status should be 200
    And Tax_Type table should have exactly 2 records in database

  Scenario: A partial edit that only carries a description keeps the other settings
    Given a tax type named "GST" exists
    When I PUT the platform path /secured/tax-types with payload:
      """json
      {"id": "{{taxType:GST}}", "description": "Goods and services tax"}
      """
    Then the response status should be 200
    And Tax_Type should match example:
      """json
      {
        "name": "GST",
        "description": "Goods and services tax",
        "calculationMethod": "PERCENTAGE",
        "taxRecoveryType": "RECOVERABLE",
        "taxApplicationLevel": "LINE_ITEM",
        "taxTriggers": ["SALE"]
      }
      """

  Scenario: An update cannot remove every trigger from a tax type
    Given a tax type named "GST" exists
    When I PUT the platform path /secured/tax-types with payload:
      """json
      {"id": "{{taxType:GST}}", "taxTriggers": []}
      """
    Then the response status should be 400
    And the response error message should be "At least one tax trigger is required"
    And Tax_Type should match example:
      """json
      {"name": "GST", "taxTriggers": ["SALE"]}
      """

  Scenario: A tax type cannot be renamed to another tax type's name
    Given a tax type named "GST" exists
    And a tax type named "VAT" exists
    When I PUT the platform path /secured/tax-types with payload:
      """json
      {"id": "{{taxType:VAT}}", "name": "gst"}
      """
    Then the response status should be 400
    And the response error message should be "A tax type named 'gst' already exists"
    And Tax_Type should match example:
      """json
      {"name": "VAT"}
      """
    And Tax_Type table should have exactly 2 records in database

  Scenario: Updating a tax type that does not exist is rejected
    When I PUT to /secured/tax-types with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "name": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Tax_Type table should have no records in database

  Scenario: Deleting an unused tax type removes it
    Given a tax type named "GST" exists
    When I DELETE the platform path /secured/tax-types/{{taxType:GST}}
    Then the response status should be 204
    And Tax_Type table should have no records in database

  Scenario: A tax type linked to a jurisdiction cannot be deleted
    Given a jurisdiction type named "Country" exists
    And a tax type named "GST" exists
    And I POST the platform path /secured/jurisdictions with payload:
      """json
      {
        "name": "Kenya",
        "jurisdictionTypeId": "{{jurisdictionType:Country}}",
        "taxTypesToAddOrReactivate": ["{{taxType:GST}}"]
      }
      """
    When I DELETE the platform path /secured/tax-types/{{taxType:GST}}
    Then the response status should be 400
    And the response error message should be "Tax type is in use and cannot be deleted"
    And Tax_Type should match example:
      """json
      {"name": "GST"}
      """
