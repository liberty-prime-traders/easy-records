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
    And response contains details:
      """json
      [{"name": "Packaging"}, {"name": "Fabric"}]
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
        {"code": "packaging-base", "name": "Piece"},
        {"code": "pal", "name": "Pallet", "baseUnitName": "Crate", "unitsOfBasePerUnit": 20},
        {"code": "crt", "name": "Crate", "baseUnitName": "Box", "unitsOfBasePerUnit": 10},
        {"code": "box", "name": "Box", "baseUnitName": "Piece", "unitsOfBasePerUnit": 6},
        {"code": "fabric-base", "name": "Piece"}
      ]
      """
    And Unit_Group table should contain exactly these records:
      """json
      [
        {"name": "Miscellaneous"},
        {"name": "Weight"},
        {"name": "Volume"},
        {"name": "Count"},
        {"name": "Packaging"},
        {"name": "Fabric"}
      ]
      """
    And Unit_Value should match example:
      """json
      {"code": "pal", "unitsOfBasePerUnit": 20}
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
    Then response contains details:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
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
        {"code": "t", "name": "Tonne", "baseUnitName": "Kilogram", "unitsOfBasePerUnit": 1000}
      ]
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """
    And Unit_Value should match example:
      """json
      {"code": "t", "unitsOfBasePerUnit": 1000}
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
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """
    And Unit_Value should not match example:
      """json
      {"code": "fine"}
      """

  Scenario: Importing the same payload twice is rejected the second time
    Given I POST to /secured/unitgroups/bulk with payload:
      """json
      {
        "unitGroups": [
          {
            "name": "Eggs",
            "unitValues": [{"name": "Dozen", "code": "egg-dz", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 12}]
          }
        ]
      }
      """
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {
        "unitGroups": [
          {
            "name": "Eggs",
            "unitValues": [{"name": "Dozen", "code": "egg-dz", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 12}]
          }
        ]
      }
      """
    Then the response status should be 400
    And response contains details:
      """json
      {
        "body": [
          "Group 'Eggs': name already exists in the database",
          "Unit value 'Dozen': name already exists in the 'Eggs' group",
          "Unit value 'egg-dz': code already exists in the database"
        ]
      }
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}, {"name": "Eggs"}]
      """
    When I GET to /secured/unitgroups
    Then response contains details:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}, {"name": "Eggs"}]
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
        {"code": "eggs-base", "name": "Piece"},
        {"code": "egg-dz", "name": "Dozen", "baseUnitName": "Piece", "unitsOfBasePerUnit": 12}
      ]
      """

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
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
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
    Then response contains details:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """

    Examples:
      | name   |
      | Weight |
      | weight |
      | VOLUME |
      | Count  |
