Feature: Bulk import of unit groups and their units
  As an authenticated organization user
  I want to load unit groups and their units in one request
  So that a whole catalog of units is saved together or not at all

  Scenario: Units are saved in chain order even when the payload lists them child first
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {
        "unitGroups": [
          {
            "name": "Packaging",
            "unitValues": [
              {"name": "Pallet", "code": "pal", "baseUnitCode": "crt", "unitsOfBasePerUnit": 20},
              {"name": "Crate", "code": "crt", "baseUnitCode": "box", "unitsOfBasePerUnit": 10},
              {"name": "Box", "code": "box", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 6}
            ]
          },
          {"name": "Fabric"}
        ]
      }
      """
    Then the response status should be 200
    And response list size is 2
    When I GET to /secured/unitvalues
    Then response contains item with details:
      """json
      {"code": "pal", "baseUnitName": "Crate", "unitsOfBasePerUnit": 20}
      """
    And response contains item with details:
      """json
      {"code": "crt", "baseUnitName": "Box", "unitsOfBasePerUnit": 10}
      """
    And response contains item with details:
      """json
      {"code": "box", "baseUnitName": "Piece", "unitsOfBasePerUnit": 6}
      """
    And response contains item with details:
      """json
      {"code": "fabric-base", "name": "Piece", "systemDefined": true}
      """

  Scenario: A payload that restates seeded system data reuses it
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {
        "unitGroups": [
          {
            "name": "Weight",
            "unitValues": [
              {"name": "Kilogram", "code": "kg", "baseUnitCode": "g", "unitsOfBasePerUnit": 1000},
              {"name": "Tonne", "code": "t", "baseUnitCode": "kg", "unitsOfBasePerUnit": 1000}
            ]
          }
        ]
      }
      """
    Then the response status should be 200
    When I GET to /secured/unitgroups
    Then the response field "findAll { it.name == 'Weight' }.size()" should be "1"
    When I GET to /secured/unitvalues
    Then the response field "findAll { it.code == 'kg' }.size()" should be "1"
    And response contains item with details:
      """json
      {"code": "t", "baseUnitName": "Kilogram", "unitsOfBasePerUnit": 1000}
      """

  Scenario: A rejected payload saves nothing and reports every problem
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {
        "unitGroups": [
          {
            "name": "Good Group",
            "unitValues": [{"name": "Fine", "code": "fine", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 2}]
          },
          {
            "name": "Bad Group",
            "unitValues": [
              {"name": "Loose", "code": "loose"},
              {"name": "Fine Again", "code": "fine", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 3}
            ]
          }
        ]
      }
      """
    Then the response status should be 400
    And the response error message should be "Bulk unit import validation failed"
    And response contains details:
      """json
      {
        "body": [
          "Unit value 'fine': duplicates another unit value code in the same payload",
          "Unit value loose: a unit in the 'Bad Group' group needs a baseUnitCode, because the group's root unit already exists"
        ]
      }
      """
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Good Group"}
      """
    And response contains no item with details:
      """json
      {"name": "Bad Group"}
      """
    When I GET to /secured/unitvalues
    Then response contains no item with details:
      """json
      {"code": "fine"}
      """
    And response contains no item with details:
      """json
      {"code": "loose"}
      """

  Scenario: Importing the same payload twice is rejected the second time
    Given I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Eggs", "unitValues": [{"name": "Dozen", "code": "egg-dz", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 12}]}]}
      """
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Eggs", "unitValues": [{"name": "Dozen", "code": "egg-dz", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 12}]}]}
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Group 'Eggs': name already exists in the database", "Unit value 'egg-dz': code already exists in the database"]}
      """
    When I GET to /secured/unitgroups
    Then the response field "findAll { it.name == 'Eggs' }.size()" should be "1"
    When I GET to /secured/unitvalues
    Then the response field "findAll { it.code == 'egg-dz' }.size()" should be "1"

  Scenario Outline: A bulk group name cannot match an existing group on letters and digits
    Given a unit group named "Pack Size" exists
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "<name>"}]}
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Group '<name>': name already exists in the database"]}
      """
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "<name>"}
      """

    Examples:
      | name        |
      | pack-size   |
      | PACK SIZE   |
      | Pack_Size   |
      | packsize    |
      | Pack  Size! |

  Scenario Outline: Two groups in one payload cannot share a name on letters and digits
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Pack Size"}, {"name": "<name>"}]}
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Group '<name>': duplicates another group name in the same payload"]}
      """
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Pack Size"}
      """

    Examples:
      | name        |
      | pack-size   |
      | PACK SIZE   |
      | Pack_Size   |
      | packsize    |
      | Pack  Size! |

  Scenario Outline: A payload naming a seeded system group in any spelling reuses it
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "<name>"}]}
      """
    Then the response status should be 200
    When I GET to /secured/unitgroups
    Then the response field "findAll { it.systemDefined }.size()" should be "4"
    And the response field "size()" should be "4"

    Examples:
      | name   |
      | Weight |
      | weight |
      | VOLUME |
      | Count  |
