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
    Then the response field "find { it.code == 'egg-root' }.name" should be "Sachet"
    And the response field "find { it.code == 'egg-hdz' }.baseUnitName" should be "Sachet"
    And the response field "find { it.code == 'egg-dz' }.baseUnitName" should be "Sachet"

  Scenario: A bulk import without a base unit code keeps the generated root unit code
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {"unitGroups": [{"name": "Milk Crates", "unitValues": []}]}
      """
    Then the response status should be 200
    When I GET to /secured/unitvalues
    Then the response field "find { it.code == 'milk-crates-base' }.name" should be "Piece"

  Scenario: Creating a single unit group creates the root unit with the explicit code
    When I POST to /secured/unitgroups with payload:
      """json
      {"name": "Boxes", "baseUnitName": "Sachet", "baseUnitCode": "box-root"}
      """
    Then the response status should be 200
    When I GET to /secured/unitvalues
    Then the response field "find { it.code == 'box-root' }.name" should be "Sachet"
    And Unit_Value should match example:
      """json
      {"code": "box-root", "name": "Sachet", "baseUnit": null, "systemDefined": true}
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
    And the response error should contain "baseUnitCode 'taken-root' already exists in the database"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Eggs"}
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
    And the response error should contain "taken-root"
    When I GET to /secured/unitgroups
    Then response contains no item with details:
      """json
      {"name": "Crates"}
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
    Then response contains item with details:
      """json
      {"code": "pallets-base", "name": "Piece", "systemDefined": true}
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
    Then the response field "findAll { !it.systemDefined }.size()" should be "0"

    Examples:
      | baseUnitCode               | problem                                                                     |
      | abcdefghijklmnopqrstuvwxyz | baseUnitCode must be at most 25 characters                                  |
      | __piece__                  | baseUnitCode '__piece__' is reserved for referencing a group's root unit    |

  Scenario: A bulk import rejects two groups with the same base unit code and saves nothing
    When I POST to /secured/unitgroups/bulk with payload:
      """json
      {
        "unitGroups": [
          {"name": "Pallets", "baseUnitCode": "pal"},
          {"name": "Crates", "baseUnitCode": "pal"}
        ]
      }
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Group 'Crates': baseUnitCode 'pal' duplicates another group's base unit code"]}
      """
    When I GET to /secured/unitgroups
    Then the response field "findAll { !it.systemDefined }.size()" should be "0"

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
    Then the response field "findAll { !it.systemDefined }.size()" should be "0"

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
    When I GET to /secured/unitgroups
    Then the response field "findAll { !it.systemDefined }.size()" should be "0"
