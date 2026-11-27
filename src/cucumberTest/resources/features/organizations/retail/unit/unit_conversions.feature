Feature: Unit conversions
  As an authenticated organization user
  I want the conversion graph to reflect the units of measure in each group
  So that every unit converts to any other unit in its group by the ratio of their sizes in the group's root unit

  Background:
    Given a unit group exists

  Scenario: A unit converts to itself at one to one
    Given a unit exists
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And the response should match json:
      """json
      {"{{unitValue}}": {"{{unitValue}}": {"name": "Test Unit", "numerator": 1, "denominator": 1}}}
      """

  Scenario: A unit converts to its group's root unit and back at its size
    Given a unit exists
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And the response should match json:
      """json
      {
        "{{rootUnit}}": {"{{unitValue}}": {"name": "Test Unit", "numerator": 1, "denominator": 12}},
        "{{unitValue}}": {"{{rootUnit}}": {"numerator": 12, "denominator": 1}}
      }
      """

  Scenario Outline: Seeded units convert at their size ratio in lowest terms
    Given the system unit "<to>" is selected
    And the system unit "<from>" is selected
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And the response should match json:
      """json
      {"{{unitValue}}": {"{{unitValue->0}}": {"name": "<toName>", "numerator": <numerator>, "denominator": <denominator>}}}
      """

    Examples:
      | from | to  | toName     | numerator | denominator |
      | kg   | g   | Gram       | 1000      | 1           |
      | g    | kg  | Kilogram   | 1         | 1000        |
      | hl   | l   | Litre      | 1         | 2           |
      | dz   | hdz | Half Dozen | 2         | 1           |

  Scenario: A Miscellaneous unit converts to itself at one to one
    Given the system unit "misc-pc" is selected
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And the response should match json:
      """json
      {"{{unitValue}}": {"{{unitValue}}": {"name": "Piece", "numerator": 1, "denominator": 1}}}
      """

  Scenario: Renaming a unit changes its name in the graph after the graph was read
    Given a unit exists
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    When I PUT to /secured/unitvalues with payload:
      """json
      {"id": "{{unitValue}}", "name": "Renamed Unit", "code": "RU"}
      """
    Then the response status should be 200
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And the response should match json:
      """json
      {"{{rootUnit}}": {"{{unitValue}}": {"name": "Renamed Unit"}}}
      """
    And Unit_Value should exist in database with id {{unitValue}} and options:
      """json
      {"name": "Renamed Unit"}
      """

  Scenario: Changing a unit's conversion factor changes both of its ratios after the graph was read
    Given a unit exists
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    When I PUT to /secured/unitvalues with payload:
      """json
      {"id": "{{unitValue}}", "name": "Test Unit", "code": "TU", "baseUnit": "{{rootUnit}}", "unitsOfBasePerUnit": 24}
      """
    Then the response status should be 200
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And the response should match json:
      """json
      {
        "{{rootUnit}}": {"{{unitValue}}": {"name": "Test Unit", "numerator": 1, "denominator": 24}},
        "{{unitValue}}": {"{{rootUnit}}": {"numerator": 24, "denominator": 1}}
      }
      """
    And Unit_Value should exist in database with id {{unitValue}} and options:
      """json
      {"unitsOfBasePerUnit": 24}
      """

  Scenario: A unit added after the graph was read appears in the graph
    Given a unit exists
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And a unit named "Other Unit" coded "OU" exists
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And the response should match json:
      """json
      {"{{unitValue->0}}": {"{{unitValue}}": {"name": "Other Unit"}}}
      """
    And Unit_Value should exist in database with id {{unitValue}} and options:
      """json
      {"code": "OU"}
      """

  Scenario: Units chained over several levels convert across the chain in lowest terms
    Given a unit group named "Beverages" with base unit "Bottle" coded "btl" exists
    And a unit named "Six-pack" coded "sixpk" equal to 6 of unit "btl" exists
    And a unit named "Crate" coded "crate" equal to 4 of unit "sixpk" exists
    And a unit named "Pallet" coded "pallet" equal to 10 of unit "crate" exists
    When I GET to /secured/unit-conversions/graph
    Then the response status should be 200
    And the response should match json:
      """json
      {
        "{{unitValue:pallet}}": {
          "{{unitValue:btl}}": {"name": "Bottle", "numerator": 240, "denominator": 1},
          "{{unitValue:sixpk}}": {"name": "Six-pack", "numerator": 40, "denominator": 1},
          "{{unitValue:crate}}": {"name": "Crate", "numerator": 10, "denominator": 1}
        },
        "{{unitValue:sixpk}}": {
          "{{unitValue:pallet}}": {"name": "Pallet", "numerator": 1, "denominator": 40}
        },
        "{{unitValue:btl}}": {
          "{{unitValue:crate}}": {"name": "Crate", "numerator": 1, "denominator": 24}
        }
      }
      """
