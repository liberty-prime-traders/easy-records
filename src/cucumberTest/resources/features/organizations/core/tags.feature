Feature: Tags
  As an authenticated organization user
  I want to label products, expenses and purchases with tags
  So that every tag has a unique name across categories

  Scenario: Creating a tag stores it with its category and description
    When I POST to /secured/tag with payload:
      """json
      {"category": "PRODUCT", "tagName": "Organic", "description": "Certified organic"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"category": "PRODUCT", "tagName": "Organic", "description": "Certified organic"}
      """
    When I GET to /secured/tag
    Then response contains details:
      """json
      [{"category": "PRODUCT", "tagName": "Organic", "description": "Certified organic"}]
      """
    And Tag table should contain exactly these records:
      """json
      [{"category": "PRODUCT", "tagName": "Organic", "description": "Certified organic"}]
      """

  Scenario Outline: A tag needs a name and a known category
    When I POST to /secured/tag with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    When I GET to /secured/tag
    Then response contains details:
      """json
      []
      """
    And Tag table should have no records in database

    Examples:
      | payload                                       | message                            |
      | {"category": "PRODUCT", "tagName": "   "}     | A tag must have a name             |
      | {"category": "PRODUCT"}                       | The request body could not be read |
      | {"tagName": "Organic"}                        | The request body could not be read |
      | {"category": "UNKNOWN", "tagName": "Organic"} | The request body could not be read |

  Scenario Outline: A tag name cannot repeat another tag's name ignoring case or spaces
    Given a tag named "Organic" exists
    When I POST to /secured/tag with payload:
      """json
      {"category": "PRODUCT", "tagName": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A tag with the name <name> already exists."
    When I GET to /secured/tag
    Then response contains details:
      """json
      [{"tagName": "Organic"}]
      """
    And Tag table should contain exactly these records:
      """json
      [{"tagName": "Organic"}]
      """

    Examples:
      | name     |
      | organic  |
      | Or ganic |

  Scenario: Tag names are unique across categories
    Given a tag named "Organic" exists
    When I POST to /secured/tag with payload:
      """json
      {"category": "EXPENSE", "tagName": "Organic"}
      """
    Then the response status should be 400
    And the response error message should be "A tag with the name Organic already exists."
    When I GET to /secured/tag
    Then response contains details:
      """json
      [{"category": "PRODUCT", "tagName": "Organic"}]
      """
    And Tag table should contain exactly these records:
      """json
      [{"category": "PRODUCT", "tagName": "Organic"}]
      """

  Scenario: Listing tags returns tags from every category
    Given a tag named "Organic" exists
    And an expense tag named "Rent" exists
    When I GET to /secured/tag
    Then response contains details:
      """json
      [{"category": "PRODUCT", "tagName": "Organic"}, {"category": "EXPENSE", "tagName": "Rent"}]
      """

  Scenario: Listing tags returns a product, an expense and a purchase tag together
    Given a tag named "Organic" exists
    And an expense tag named "Rent" exists
    When I POST to /secured/tag with payload:
      """json
      {"category": "PURCHASE", "tagName": "Supplier Invoice"}
      """
    Then the response status should be 200
    When I GET to /secured/tag
    Then response contains details:
      """json
      [
        {"category": "PRODUCT", "tagName": "Organic"},
        {"category": "EXPENSE", "tagName": "Rent"},
        {"category": "PURCHASE", "tagName": "Supplier Invoice"}
      ]
      """
    And Tag table should contain exactly these records:
      """json
      [
        {"category": "PRODUCT", "tagName": "Organic"},
        {"category": "EXPENSE", "tagName": "Rent"},
        {"category": "PURCHASE", "tagName": "Supplier Invoice"}
      ]
      """

  Scenario: Updating a tag that does not exist is rejected
    Given a tag named "Organic" exists
    When I PUT to /secured/tag with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "tagName": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Tag table should contain exactly these records:
      """json
      [{"category": "PRODUCT", "tagName": "Organic"}]
      """

  Scenario Outline: An update may not blank or null the name, and is rejected before the tag is looked up
    Given a tag named "Organic" exists
    When I PUT to /secured/tag with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "A tag must have a name"
    And Tag table should contain exactly these records:
      """json
      [{"category": "PRODUCT", "tagName": "Organic"}]
      """

    Examples:
      | payload                                                                     |
      | {"id": "00000000-0000-0000-0000-000000000001", "tagName": "   "}            |
      | {"id": "00000000-0000-0000-0000-000000000001", "tagName": null}             |

  Scenario: Deleting a tag with an id that is not a UUID is rejected
    Given a tag named "Organic" exists
    When I DELETE to /secured/tag/not-a-uuid
    Then the response status should be 400
    And the response error message should be "Invalid value for id"
    And Tag table should contain exactly these records:
      """json
      [{"category": "PRODUCT", "tagName": "Organic"}]
      """

  Scenario: Deleting a tag that does not exist changes nothing
    Given a tag named "Organic" exists
    When I DELETE to /secured/tag/00000000-0000-0000-0000-000000000001
    Then the response status should be 204
    When I GET to /secured/tag
    Then response contains details:
      """json
      [{"tagName": "Organic"}]
      """
    And Tag table should contain exactly these records:
      """json
      [{"category": "PRODUCT", "tagName": "Organic"}]
      """
