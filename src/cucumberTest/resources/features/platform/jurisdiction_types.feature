Feature: Jurisdiction types
  As a platform admin
  I want to define the kinds of jurisdiction the platform sets up
  So that every jurisdiction is classified by a type with a unique name

  Background:
    Given I am authenticated as a platform admin

  Scenario: Creating a jurisdiction type stores it and lists it
    When I POST to /secured/jurisdiction-types with payload:
      """json
      {"name": "Province", "description": "First-level division"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Province", "description": "First-level division"}
      """
    When I GET to /secured/jurisdiction-types
    Then response contains details:
      """json
      [{"name": "Province", "description": "First-level division"}]
      """
    And Jurisdiction_Type should match example:
      """json
      {"name": "Province", "description": "First-level division"}
      """

  Scenario Outline: A jurisdiction type needs a name
    When I POST to /secured/jurisdiction-types with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "Name must not be blank"
    And Jurisdiction_Type table should have no records in database

    Examples:
      | payload         |
      | {"name": ""}    |
      | {"name": "   "} |

  Scenario Outline: Jurisdiction type names are unique ignoring case and spacing
    Given a jurisdiction type named "Province" exists
    When I POST to /secured/jurisdiction-types with payload:
      """json
      {"name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A jurisdiction type named '<name>' already exists"
    And Jurisdiction_Type table should contain exactly these records:
      """json
      [{"name": "Province"}]
      """

    Examples:
      | name       |
      | province   |
      | Pro vince  |

  Scenario: Renaming a jurisdiction type frees its old name
    Given a jurisdiction type named "Province" exists
    When I POST to /secured/jurisdiction-types with payload:
      """json
      {"name": "Province"}
      """
    Then the response status should be 400
    And the response error message should be "A jurisdiction type named 'Province' already exists"
    When I PUT the platform path /secured/jurisdiction-types with payload:
      """json
      {"id": "{{jurisdictionType:Province}}", "name": "Territory"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Territory"}
      """
    When I POST to /secured/jurisdiction-types with payload:
      """json
      {"name": "Province"}
      """
    Then the response status should be 200
    And Jurisdiction_Type table should contain exactly these records:
      """json
      [{"name": "Territory"}, {"name": "Province"}]
      """

  Scenario: An update that only carries a description keeps the name
    Given a jurisdiction type named "Province" exists
    When I PUT the platform path /secured/jurisdiction-types with payload:
      """json
      {"id": "{{jurisdictionType:Province}}", "description": "First-level division"}
      """
    Then the response status should be 200
    And Jurisdiction_Type should match example:
      """json
      {"name": "Province", "description": "First-level division"}
      """

  Scenario: A null description clears the jurisdiction type's description
    Given a jurisdiction type named "Province" exists
    And I PUT the platform path /secured/jurisdiction-types with payload:
      """json
      {"id": "{{jurisdictionType:Province}}", "description": "Temporary"}
      """
    When I PUT the platform path /secured/jurisdiction-types with payload:
      """json
      {"id": "{{jurisdictionType:Province}}", "description": null}
      """
    Then the response status should be 200
    And Jurisdiction_Type should match example:
      """json
      {"name": "Province", "description": null}
      """

  Scenario: An update cannot clear the jurisdiction type name
    Given a jurisdiction type named "Province" exists
    When I PUT the platform path /secured/jurisdiction-types with payload:
      """json
      {"id": "{{jurisdictionType:Province}}", "name": ""}
      """
    Then the response status should be 400
    And the response error message should be "Name must not be blank"
    And Jurisdiction_Type should match example:
      """json
      {"name": "Province"}
      """

  Scenario Outline: A jurisdiction type cannot be renamed to another type's name
    Given a jurisdiction type named "Province" exists
    And a jurisdiction type named "Territory" exists
    When I PUT the platform path /secured/jurisdiction-types with payload:
      """json
      {"id": "{{jurisdictionType:Territory}}", "name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A jurisdiction type named '<name>' already exists"
    And Jurisdiction_Type table should contain exactly these records:
      """json
      [{"name": "Territory"}, {"name": "Province"}]
      """

    Examples:
      | name      |
      | province  |
      | Pro vince |

  Scenario: Updating a jurisdiction type that does not exist is rejected
    When I PUT to /secured/jurisdiction-types with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "name": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Jurisdiction_Type table should have no records in database

  Scenario: Deleting a jurisdiction type removes it
    Given a jurisdiction type named "Province" exists
    When I DELETE the platform path /secured/jurisdiction-types/{{jurisdictionType:Province}}
    Then the response status should be 204
    And Jurisdiction_Type table should have no records in database
