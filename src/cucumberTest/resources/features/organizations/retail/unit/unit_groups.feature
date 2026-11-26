Feature: Unit groups
  As an authenticated organization user
  I want to manage the unit groups that hold my units of measure
  So that every group has a root unit and no two groups share a name

  Scenario: Creating a unit group provisions its root unit
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Boxes", "description": "Things sold in boxes"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Boxes"}
      """
    When I GET to /secured/unitgroups
    Then response contains details:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}, {"name": "Boxes"}]
      """
    When I GET to /secured/unitvalues
    Then response contains details:
      """json
      [
        {"code": "g", "name": "Gram"},
        {"code": "kg", "name": "Kilogram"},
        {"code": "ml", "name": "Millilitre"},
        {"code": "l", "name": "Litre"},
        {"code": "hl", "name": "Half Litre"},
        {"code": "pc", "name": "Piece"},
        {"code": "dz", "name": "Dozen"},
        {"code": "hdz", "name": "Half Dozen"},
        {"code": "misc-pc", "name": "Piece"},
        {"code": "boxes-base", "name": "Piece"}
      ]
      """
    And Unit_Group should match example:
      """json
      {"name": "Boxes"}
      """
    And Unit_Value should match example:
      """json
      {"code": "boxes-base"}
      """

  Scenario Outline: A unit group name needs a letter or digit
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """

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
      """json
      {"name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "An UnitGroup using the name '<name>' already exists"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "<name>"}
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}, {"name": "Pack Size"}]
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
      """json
      {"name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "An UnitGroup using the name '<name>' already exists"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "<name>"}
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """

    Examples:
      | name    |
      | weight  |
      | VOLUME  |
      | Wei ght |

  Scenario: A renamed group's old name can be reused when the new group gives its own root code
    Given a unit group exists
    When I PUT to /secured/unitgroups with payload:
      """json
      {"id": "{{unitGroup}}", "name": "Renamed Group"}
      """
    Then the response status should be 200
    When I GET to /secured/unitgroups
    Then response contains details:
      """json
      [
        {"name": "Miscellaneous"},
        {"name": "Weight"},
        {"name": "Volume"},
        {"name": "Count"},
        {"id": "{{unitGroup}}", "name": "Renamed Group"}
      ]
      """
    And response contains no item with details:
      """json
      {"name": "Test Unit Group"}
      """
    And Unit_Group should exist in database with id {{unitGroup}} and options:
      """json
      {"name": "Renamed Group"}
      """
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Test Unit Group", "baseUnitCode": "reused-root"}
      """
    Then the response status should be 200
    When I GET to /secured/unitgroups
    Then response contains details:
      """json
      [
        {"name": "Miscellaneous"},
        {"name": "Weight"},
        {"name": "Volume"},
        {"name": "Count"},
        {"id": "{{unitGroup}}", "name": "Renamed Group"},
        {"name": "Test Unit Group"}
      ]
      """
    And Unit_Group should match example:
      """json
      {"name": "Test Unit Group"}
      """
    And Unit_Value should match example:
      """json
      {"code": "reused-root"}
      """

  Scenario: A renamed group keeps its root code, so the old name is rejected until a new code is given
    Given a unit group exists
    And I PUT to /secured/unitgroups with payload:
      """json
      {"id": "{{unitGroup}}", "name": "Renamed Group"}
      """
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Test Unit Group"}
      """
    Then the response status should be 400
    And the response error message should be "A unit value with the code test-unit-group-base already exists"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Test Unit Group"}
      """
    And Unit_Value should match example:
      """json
      {"code": "test-unit-group-base", "unitGroupId": "{{unitGroup}}"}
      """

  Scenario: An update that only carries a description keeps the name
    Given a unit group exists
    When I PUT to /secured/unitgroups with payload:
      """json
      {"id": "{{unitGroup}}", "description": "Updated description"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"id": "{{unitGroup}}", "name": "Test Unit Group", "description": "Updated description"}
      """
    And Unit_Group should exist in database with id {{unitGroup}} and options:
      """json
      {"name": "Test Unit Group", "description": "Updated description"}
      """

  Scenario: A null description clears the group's description
    Given a unit group exists
    And I PUT to /secured/unitgroups with payload:
      """json
      {"id": "{{unitGroup}}", "description": "Temporary"}
      """
    When I PUT to /secured/unitgroups with payload:
      """json
      {"id": "{{unitGroup}}", "description": null}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"id": "{{unitGroup}}", "name": "Test Unit Group", "description": null}
      """
    And Unit_Group should exist in database with id {{unitGroup}} and options:
      """json
      {"description": null}
      """

  Scenario Outline: A unit group cannot be renamed to another group's name
    Given a unit group named "Pack Size" exists
    And a unit group named "Boxes" exists
    When I PUT to /secured/unitgroups with payload:
      """json
      {"id": "{{unitGroup}}", "name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "An UnitGroup using the name '<name>' already exists"
    And Unit_Group should exist in database with id {{unitGroup}} and options:
      """json
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
      """json
      {"id": "{{unitGroup}}", "name": "Heavy Things"}
      """
    Then the response status should be 400
    And the response error message should be "System-defined unit groups cannot be modified"
    And Unit_Group should exist in database with id {{unitGroup}} and options:
      """json
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
    When I DELETE to /secured/unitgroups/{{unitGroup}}
    Then the response status should be 400
    And the response error message should be "System-defined unit groups cannot be deleted"
    And Unit_Group should exist in database with id {{unitGroup}}

    Examples:
      | group         |
      | Weight        |
      | Volume        |
      | Count         |
      | Miscellaneous |

  Scenario: Deleting a unit group also removes its root unit
    Given a unit group exists
    When I DELETE to /secured/unitgroups/{{unitGroup}}
    Then the response status should be 204
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Test Unit Group"}
      """
    When I GET to /secured/unitvalues
    Then response contains no item with details:
      """json
      {"code": "test-unit-group-base"}
      """
    And Unit_Group should not exist in database with id {{unitGroup}}
    And Unit_Value should not exist in database with id {{rootUnit}}

  Scenario: A unit group with a user-defined unit cannot be deleted
    Given a unit group exists
    And a unit exists
    When I DELETE to /secured/unitgroups/{{unitGroup}}
    Then the response status should be 400
    And the response error message should be "A unit group with user-defined units cannot be deleted; delete those units first"
    And Unit_Group should exist in database with id {{unitGroup}}
    When I GET to /secured/unitgroups
    Then response contains details:
      """json
      [
        {"name": "Miscellaneous"},
        {"name": "Weight"},
        {"name": "Volume"},
        {"name": "Count"},
        {"id": "{{unitGroup}}", "name": "Test Unit Group"}
      ]
      """
