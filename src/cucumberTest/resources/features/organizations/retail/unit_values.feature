Feature: Unit values
  As an authenticated organization user
  I want to manage the units of measure inside a unit group
  So that every unit converts to its group's root unit

  Background:
    Given a unit group exists

  Scenario: Creating a unit based on the group's root unit
    When I POST to /secured/unitvalues with payload:
      """
      {"name": "Dozen", "code": "doz", "unitGroupId": "#unitGroup", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": 12}
      """
    Then the response status should be 200
    And response contains details:
      """
      {"name": "Dozen", "code": "doz", "unitGroupId": "#unitGroup", "baseUnitName": "Piece", "unitsOfBasePerUnit": 12, "systemDefined": false}
      """
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup
    Then response contains item with details:
      """
      {"code": "doz"}
      """
    And Unit_Value should match example:
      """
      {"name": "Dozen", "code": "doz", "unitGroupId": "#unitGroup", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": 12, "systemDefined": false}
      """

  Scenario: Unit names are scoped to their group
    Given a unit exists
    And a unit group named "Second Group" exists
    When I POST to /secured/unitvalues with payload:
      """
      {"name": "Test Unit", "code": "TU2", "unitGroupId": "#unitGroup", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": 3}
      """
    Then the response status should be 200
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup
    Then response contains item with details:
      """
      {"name": "Test Unit", "code": "TU2"}
      """

  Scenario Outline: A unit name cannot repeat inside its group
    Given a unit exists
    When I POST to /secured/unitvalues with payload:
      """
      {"name": "<name>", "code": "TU2", "unitGroupId": "#unitGroup", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": 3}
      """
    Then the response status should be 400
    And the response error message should be "A unit value with the name <name> already exists"
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup
    Then response contains no item with details:
      """
      {"code": "TU2"}
      """

    Examples:
      | name      |
      | test-unit |
      | TEST UNIT |
      | Test_Unit |
      | testunit  |

  Scenario: Unit codes are unique across groups
    Given a unit exists
    And a unit group named "Second Group" exists
    When I POST to /secured/unitvalues with payload:
      """
      {"name": "Other Unit", "code": "TU", "unitGroupId": "#unitGroup", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": 3}
      """
    Then the response status should be 400
    And the response error message should be "A unit value with the code TU already exists"
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup
    Then response contains no item with details:
      """
      {"name": "Other Unit"}
      """

  Scenario Outline: A unit code cannot reuse a seeded system unit code
    When I POST to /secured/unitvalues with payload:
      """
      {"name": "Mine", "code": "<code>", "unitGroupId": "#unitGroup", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": 3}
      """
    Then the response status should be 400
    And the response error message should be "A unit value with the code <code> already exists"
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup
    Then response contains no item with details:
      """
      {"name": "Mine"}
      """

    Examples:
      | code    |
      | g       |
      | kg      |
      | l       |
      | dz      |
      | misc-pc |

  Scenario Outline: A conversion factor must be a positive size the chain can hold
    When I POST to /secured/unitvalues with payload:
      """
      {"name": "Odd Unit", "code": "odd", "unitGroupId": "#unitGroup", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": <factor>}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup
    Then response contains no item with details:
      """
      {"code": "odd"}
      """

    Examples:
      | factor     | message                                                           |
      | 0          | unitsOfBasePerUnit must be a positive whole number                |
      | -5         | unitsOfBasePerUnit must be a positive whole number                |
      | 1000000001 | A unit cannot be larger than 1,000,000,000 of its group's root unit |

  Scenario: Renaming a unit keeps its place in the chain
    Given a unit exists
    When I PUT to /secured/unitvalues with payload:
      """
      {"id": "#unitValue", "name": "Renamed Unit", "code": "RU"}
      """
    Then the response status should be 200
    And response contains details:
      """
      {"name": "Renamed Unit", "code": "RU", "baseUnitName": "Piece", "unitsOfBasePerUnit": 12}
      """
    And Unit_Value should exist in database with id #unitValue and options:
      """
      {"name": "Renamed Unit", "code": "RU", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": 12}
      """

  Scenario: Changing a unit's conversion factor
    Given a unit exists
    When I PUT to /secured/unitvalues with payload:
      """
      {"id": "#unitValue", "name": "Test Unit", "code": "TU", "baseUnit": "#rootUnit", "unitsOfBasePerUnit": 24}
      """
    Then the response status should be 200
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup
    Then response contains item with details:
      """
      {"id": "#unitValue", "unitsOfBasePerUnit": 24}
      """
    And Unit_Value should exist in database with id #unitValue and options:
      """
      {"unitsOfBasePerUnit": 24}
      """

  Scenario Outline: A seeded system unit cannot be modified
    Given the system unit "<code>" is selected
    When I PUT to /secured/unitvalues with payload:
      """
      {"id": "#unitValue", "name": "Renamed", "code": "<code>"}
      """
    Then the response status should be 400
    And the response error message should be "System-defined unit values cannot be modified"
    And Unit_Value should exist in database with id #unitValue and options:
      """
      {"code": "<code>", "systemDefined": true}
      """

    Examples:
      | code    |
      | g       |
      | kg      |
      | hl      |
      | misc-pc |

  Scenario Outline: A seeded system unit cannot be deleted
    Given the system unit "<code>" is selected
    When I DELETE to /secured/unitvalues/#unitValue
    Then the response status should be 400
    And the response error message should be "System-defined unit values cannot be deleted"
    And Unit_Value should exist in database with id #unitValue

    Examples:
      | code    |
      | g       |
      | kg      |
      | hl      |
      | misc-pc |

  Scenario: The root unit of a group cannot be modified
    When I PUT to /secured/unitvalues with payload:
      """
      {"id": "#rootUnit", "name": "Each", "code": "each"}
      """
    Then the response status should be 400
    And the response error message should be "System-defined unit values cannot be modified"
    And Unit_Value should exist in database with id #rootUnit and options:
      """
      {"name": "Piece", "code": "test-unit-group-base"}
      """

  Scenario: The root unit of a group cannot be deleted
    When I DELETE to /secured/unitvalues/#rootUnit
    Then the response status should be 400
    And the response error message should be "System-defined unit values cannot be deleted"
    And Unit_Value should exist in database with id #rootUnit

  Scenario: Deleting a unit leaves the rest of the group
    Given a unit exists
    When I DELETE to /secured/unitvalues/#unitValue
    Then the response status should be 204
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup
    Then response contains no item with details:
      """
      {"code": "TU"}
      """
    And response contains item with details:
      """
      {"id": "#rootUnit"}
      """
    And Unit_Value should not exist in database with id #unitValue

  Scenario: A unit that is the base of another unit cannot be deleted
    Given a unit exists
    And I POST to /secured/unitvalues with payload:
      """
      {"name": "Case", "code": "CS", "unitGroupId": "#unitGroup", "baseUnit": "#unitValue", "unitsOfBasePerUnit": 10}
      """
    When I DELETE to /secured/unitvalues/#unitValue
    Then the response status should be 400
    And the response error message should be "A unit that is the base unit of another unit cannot be deleted"
    And Unit_Value should exist in database with id #unitValue

  Scenario: Listing units can be narrowed to one group
    Given a unit exists
    And a unit group named "Second Group" exists
    And a unit named "Other Unit" coded "OU" exists
    When I GET to /secured/unitvalues?unitGroupId=#unitGroup->0
    Then response contains item with details:
      """
      {"code": "TU"}
      """
    And response contains no item with details:
      """
      {"code": "OU"}
      """
    When I GET to /secured/unitvalues
    Then response contains item with details:
      """
      {"code": "OU"}
      """

  Scenario: Unauthenticated users cannot list unit values
    Given I am not authenticated
    When I GET to /secured/unitvalues
    Then the response status should be 403
