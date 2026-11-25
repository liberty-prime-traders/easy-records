Feature: Jurisdictions
  As a platform admin
  I want to set up jurisdictions under a jurisdiction type
  So that each jurisdiction has a unique type, a valid parent and a clear hierarchy

  Background:
    Given I am authenticated as a platform admin

  Scenario: Creating a jurisdiction stores it under its type and lists it with the type name
    Given a jurisdiction type named "Country" exists
    When I POST the platform path /secured/jurisdictions with payload:
      """json
      {"name": "Kenya", "jurisdictionTypeId": "{{jurisdictionType:Country}}"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Kenya", "jurisdictionType": "Country"}
      """
    When I GET to /secured/jurisdictions
    Then response contains details:
      """json
      [{"name": "Kenya", "jurisdictionType": "Country"}]
      """
    And Jurisdiction should match example:
      """json
      {"name": "Kenya"}
      """

  Scenario Outline: A jurisdiction needs a name
    Given a jurisdiction type named "Country" exists
    When I POST the platform path /secured/jurisdictions with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "Name must not be blank"
    And Jurisdiction table should have no records in database

    Examples:
      | payload                                                            |
      | {"name": "", "jurisdictionTypeId": "{{jurisdictionType:Country}}"}   |
      | {"name": "   ", "jurisdictionTypeId": "{{jurisdictionType:Country}}"} |

  Scenario: A jurisdiction must reference a jurisdiction type that exists
    When I POST to /secured/jurisdictions with payload:
      """json
      {"name": "Kenya", "jurisdictionTypeId": "00000000-0000-0000-0000-000000000001"}
      """
    Then the response status should be 400
    And the response error message should be "Jurisdiction type not found"
    And Jurisdiction table should have no records in database

  Scenario: A jurisdiction's parent must exist
    Given a jurisdiction type named "Country" exists
    When I POST the platform path /secured/jurisdictions with payload:
      """json
      {
        "name": "Nairobi",
        "jurisdictionTypeId": "{{jurisdictionType:Country}}",
        "parentJurisdictionId": "00000000-0000-0000-0000-000000000001"
      }
      """
    Then the response status should be 400
    And the response error message should be "Parent jurisdiction not found"
    And Jurisdiction table should have no records in database

  Scenario: A jurisdiction placed under a parent shows the parent's name
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    When I POST the platform path /secured/jurisdictions with payload:
      """json
      {
        "name": "Nairobi",
        "jurisdictionTypeId": "{{jurisdictionType:Country}}",
        "parentJurisdictionId": "{{jurisdiction:Kenya}}"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Nairobi", "parentJurisdiction": "Kenya"}
      """
    And Jurisdiction should match example:
      """json
      {"name": "Nairobi"}
      """

  Scenario: Renaming a jurisdiction keeps its type
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    When I PUT the platform path /secured/jurisdictions with payload:
      """json
      {"id": "{{jurisdiction:Kenya}}", "name": "Republic of Kenya"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Republic of Kenya", "jurisdictionType": "Country"}
      """
    And Jurisdiction should match example:
      """json
      {"name": "Republic of Kenya"}
      """

  Scenario: A parent cannot be a descendant of the jurisdiction it would move under
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    And a jurisdiction named "Nairobi" exists with parent "Kenya"
    When I PUT the platform path /secured/jurisdictions with payload:
      """json
      {"id": "{{jurisdiction:Kenya}}", "parentJurisdictionId": "{{jurisdiction:Nairobi}}"}
      """
    Then the response status should be 400
    And the response error message should be "Circular hierarchy detected: Nairobi cannot be set as parent of Kenya because it is a descendant of Kenya"
    And Jurisdiction should match example:
      """json
      {"name": "Kenya", "parentJurisdictionId": null}
      """

  Scenario: Deleting a jurisdiction removes it
    Given a jurisdiction type named "Country" exists
    And a jurisdiction named "Kenya" exists
    When I DELETE the platform path /secured/jurisdictions/{{jurisdiction:Kenya}}
    Then the response status should be 204
    And Jurisdiction table should have no records in database
