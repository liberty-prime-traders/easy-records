@unit-groups
Feature: Unit groups
  As an authenticated organization user
  I want to manage the unit groups that hold my units of measure
  So that every group has a root unit and no two groups share a name

  Scenario: Creating a unit group provisions its root unit
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "Boxes", "description": "Things sold in boxes"}
      """
    Then the response status should be 200
    And response contains details:
      """
      {"name": "Boxes", "description": "Things sold in boxes", "systemDefined": false}
      """
    When I GET to /secured/unitgroups
    Then response contains item with details:
      """
      {"name": "Boxes"}
      """
    When I GET to /secured/unitvalues
    Then response contains item with details:
      """
      {"code": "boxes-base", "name": "Piece", "systemDefined": true}
      """

  Scenario Outline: A unit group name needs a letter or digit
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "<message>"

    Examples:
      | name | message                                                        |
      |      | An UnitGroup must have a name                                  |
      | ***  | A unit group name must contain at least one letter or digit    |
      | ---  | A unit group name must contain at least one letter or digit    |
      | _ _  | A unit group name must contain at least one letter or digit    |
      | !?   | A unit group name must contain at least one letter or digit    |

  Scenario Outline: Group names are unique on their letters and digits alone
    Given a unit group named "Pack Size" exists
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "An UnitGroup using the name '<name>' already exists"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """
      {"name": "<name>"}
      """

    Examples:
      | name        |
      | pack-size   |
      | PACK SIZE   |
      | Pack_Size   |
      | packsize    |
      | Pack  Size! |

  Scenario Outline: A new group cannot take the name of a seeded system group
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "An UnitGroup using the name '<name>' already exists"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """
      {"name": "<name>"}
      """

    Examples:
      | name    |
      | weight  |
      | VOLUME  |
      | Wei ght |

  Scenario: A renamed group's old name can be reused when the new group gives its own root code
    Given a unit group exists
    When I PUT to /secured/unitgroups with payload:
      """
      {"id": "#unitGroup", "name": "Renamed Group"}
      """
    Then the response status should be 200
    When I GET to /secured/unitgroups
    Then response contains item with details:
      """
      {"id": "#unitGroup", "name": "Renamed Group"}
      """
    And response contains no item with details:
      """
      {"name": "Test Unit Group"}
      """
    And Unit_Group should exist in database with id #unitGroup and options:
      """
      {"name": "Renamed Group", "nameKey": "renamedgroup"}
      """
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "Test Unit Group", "baseUnitCode": "reused-root"}
      """
    Then the response status should be 200
    When I GET to /secured/unitgroups
    Then the response field "findAll { it.name == 'Test Unit Group' }.size()" should be "1"
    And the response field "findAll { it.name == 'Renamed Group' }.size()" should be "1"

  Scenario: A renamed group keeps its root code, so the old name is rejected until a new code is given
    Given a unit group exists
    And I PUT to /secured/unitgroups with payload:
      """
      {"id": "#unitGroup", "name": "Renamed Group"}
      """
    When I POST to /secured/unitgroups with payload:
      """
      {"name": "Test Unit Group"}
      """
    Then the response status should be 400
    And the response error message should be "A unit value with the code test-unit-group-base already exists"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """
      {"name": "Test Unit Group"}
      """
    And Unit_Value should match example:
      """
      {"code": "test-unit-group-base", "unitGroupId": "#unitGroup"}
      """

  Scenario: An update that only carries a description keeps the name
    Given a unit group exists
    When I PUT to /secured/unitgroups with payload:
      """
      {"id": "#unitGroup", "description": "Updated description"}
      """
    Then the response status should be 200
    And response contains details:
      """
      {"name": "Test Unit Group", "description": "Updated description"}
      """
    And Unit_Group should exist in database with id #unitGroup and options:
      """
      {"name": "Test Unit Group", "nameKey": "testunitgroup", "description": "Updated description"}
      """

  Scenario: A null description clears the group's description
    Given a unit group exists
    And I PUT to /secured/unitgroups with payload:
      """
      {"id": "#unitGroup", "description": "Temporary"}
      """
    When I PUT to /secured/unitgroups with payload:
      """
      {"id": "#unitGroup", "description": null}
      """
    Then the response status should be 200
    And response contains details:
      """
      {"name": "Test Unit Group"}
      """
    And Unit_Group should exist in database with id #unitGroup and options:
      """
      {"name": "Test Unit Group", "description": null}
      """

  Scenario Outline: A unit group cannot be renamed to another group's name
    Given a unit group named "Pack Size" exists
    And a unit group named "Boxes" exists
    When I PUT to /secured/unitgroups with payload:
      """
      {"id": "#unitGroup", "name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "An UnitGroup using the name '<name>' already exists"
    And Unit_Group should exist in database with id #unitGroup and options:
      """
      {"name": "Boxes"}
      """

    Examples:
      | name        |
      | pack-size   |
      | PACK SIZE   |
      | Pack_Size   |
      | packsize    |
      | Pack  Size! |

  Scenario Outline: A seeded system group cannot be modified
    Given the system unit group "<group>" is selected
    When I PUT to /secured/unitgroups with payload:
      """
      {"id": "#unitGroup", "name": "Heavy Things"}
      """
    Then the response status should be 400
    And the response error message should be "System-defined unit groups cannot be modified"
    And Unit_Group should exist in database with id #unitGroup and options:
      """
      {"name": "<group>"}
      """

    Examples:
      | group         |
      | Weight        |
      | Volume        |
      | Count         |
      | Miscellaneous |

  Scenario Outline: A seeded system group cannot be deleted
    Given the system unit group "<group>" is selected
    When I DELETE to /secured/unitgroups/#unitGroup
    Then the response status should be 400
    And the response error message should be "System-defined unit groups cannot be deleted"
    And Unit_Group should exist in database with id #unitGroup

    Examples:
      | group         |
      | Weight        |
      | Volume        |
      | Count         |
      | Miscellaneous |

  Scenario: Deleting a unit group also removes its root unit
    Given a unit group exists
    When I DELETE to /secured/unitgroups/#unitGroup
    Then the response status should be 204
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """
      {"name": "Test Unit Group"}
      """
    When I GET to /secured/unitvalues
    Then response contains no item with details:
      """
      {"code": "test-unit-group-base"}
      """
    And Unit_Group should not exist in database with id #unitGroup
    And Unit_Value should not exist in database with id #rootUnit

  Scenario: A unit group with a user-defined unit cannot be deleted
    Given a unit group exists
    And a unit exists
    When I DELETE to /secured/unitgroups/#unitGroup
    Then the response status should be 400
    And the response error message should be "A unit group with user-defined units cannot be deleted; delete those units first"
    When I GET to /secured/unitgroups
    Then response contains item with details:
      """
      {"name": "Test Unit Group"}
      """
