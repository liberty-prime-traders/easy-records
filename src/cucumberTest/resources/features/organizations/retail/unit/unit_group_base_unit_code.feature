Feature: Unit group base unit code
  As an authenticated organization user
  I want to choose the code of a custom unit group's base unit
  So that products can reference it without reproducing the generated code

  Scenario: A bulk import creates the root unit with the explicit code and resolves both reference styles
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {
        "unitGroups": [
          {
            "name": "Eggs",
            "baseUnitName": "Sachet",
            "baseUnitCode": "egg-root",
            "unitValues": [
              {"name": "Half Dozen", "code": "egg-hdz", "baseUnitCode": "egg-root", "unitsOfBasePerUnit": 6},
              {"name": "Dozen", "code": "egg-dz", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 12}
            ]
          }
        ]
      }
      """
    Then the response status should be 200
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
        {"code": "egg-root", "name": "Sachet"},
        {"code": "egg-hdz", "name": "Half Dozen", "baseUnitName": "Sachet", "unitsOfBasePerUnit": 6},
        {"code": "egg-dz", "name": "Dozen", "baseUnitName": "Sachet", "unitsOfBasePerUnit": 12}
      ]
      """
    And Unit_Group should match example:
      """json
      {"name": "Eggs"}
      """
    And Unit_Value should match example:
      """json
      {"code": "egg-root"}
      """

  Scenario: A bulk import without a base unit code keeps the generated root unit code
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Milk Crates", "unitValues": []}]}
      """
    Then the response status should be 200
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
        {"code": "milk-crates-base", "name": "Piece"}
      ]
      """
    And Unit_Group should match example:
      """json
      {"name": "Milk Crates"}
      """

  Scenario: Creating a single unit group creates the root unit with the explicit code
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Boxes", "baseUnitName": "Sachet", "baseUnitCode": "box-root"}
      """
    Then the response status should be 200
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
        {"code": "box-root", "name": "Sachet"}
      ]
      """
    And Unit_Value should match example:
      """json
      {"code": "box-root"}
      """

  Scenario: A bulk import rejects a base unit code that already exists in the database
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Boxes", "baseUnitCode": "taken-root"}
      """
    Then the response status should be 200
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Eggs", "baseUnitCode": "taken-root"}]}
      """
    Then the response status should be 400
    And the response error message should be "Bulk unit import validation failed"
    And response contains details:
      """json
      {"body": ["Group 'Eggs': baseUnitCode 'taken-root' already exists in the database"]}
      """
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Eggs"}
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}, {"name": "Boxes"}]
      """

  Scenario: Creating a single unit group rejects a base unit code that already exists in the database
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Boxes", "baseUnitCode": "taken-root"}
      """
    Then the response status should be 200
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Crates", "baseUnitCode": "taken-root"}
      """
    Then the response status should be 400
    And the response error message should be "A unit value with the code taken-root already exists"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Crates"}
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}, {"name": "Boxes"}]
      """

  Scenario Outline: A single unit group rejects a malformed base unit code and is not saved
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Pallets", "baseUnitCode": "<code>"}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Pallets"}
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """

    Examples:
      | code                       | message                                         |
      | __piece__                  | The unit value code __piece__ is reserved       |
      | abcdefghijklmnopqrstuvwxyz | A unit value code must be at most 25 characters |

  Scenario: A blank base unit code falls back to the generated root unit code
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Pallets", "baseUnitCode": "   "}
      """
    Then the response status should be 200
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
        {"code": "pallets-base", "name": "Piece"}
      ]
      """
    And Unit_Group should match example:
      """json
      {"name": "Pallets"}
      """

  Scenario Outline: A bulk import rejects a malformed base unit code and saves nothing
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Pallets", "baseUnitCode": "<baseUnitCode>"}]}
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Group 'Pallets': <problem>"]}
      """
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
      | baseUnitCode               | problem                                                                     |
      | abcdefghijklmnopqrstuvwxyz | baseUnitCode must be at most 25 characters                                  |
      | __piece__                  | baseUnitCode '__piece__' is reserved for referencing a group's root unit    |

  Scenario: A bulk import rejects two groups with the same base unit code and saves nothing
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Pallets", "baseUnitCode": "pal"}, {"name": "Crates", "baseUnitCode": "pal"}]}
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Group 'Crates': baseUnitCode 'pal' duplicates another group's base unit code"]}
      """
    When I GET to /secured/unitgroups
    Then response contains details:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """

  Scenario: A bulk import rejects a base unit code that a unit value in the payload already uses
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {
        "unitGroups": [
          {
            "name": "Pallets",
            "baseUnitCode": "pal",
            "unitValues": [{"name": "Big", "code": "pal", "baseUnitCode": "__piece__", "unitsOfBasePerUnit": 2}]
          }
        ]
      }
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Group 'Pallets': baseUnitCode 'pal' duplicates a unit value code in the same payload"]}
      """
    When I GET to /secured/unitgroups
    Then response contains details:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """

  Scenario: Two long group names cannot generate the same base unit code
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Extra Long Group Name Alpha"}, {"name": "Extra Long Group Name Beta"}]}
      """
    Then the response status should be 400
    And response contains details:
      """json
      {
        "body": [
          "Group 'Extra Long Group Name Beta': its generated base unit code 'extra-long-group-nam-base' collides with another unit code; choose a name that differs earlier in the text, or supply a baseUnitCode"
        ]
      }
      """
    And Unit_Group table should contain exactly these records:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """
    When I GET to /secured/unitgroups
    Then response contains details:
      """json
      [{"name": "Miscellaneous"}, {"name": "Weight"}, {"name": "Volume"}, {"name": "Count"}]
      """
