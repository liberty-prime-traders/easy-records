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
    Then response contains item with details:
      """json
      {"category": "PRODUCT", "tagName": "Organic"}
      """
    And Tag should match example:
      """json
      {"category": "PRODUCT", "tagName": "Organic", "description": "Certified organic"}
      """
    And Tag table should have exactly 1 records in database

  Scenario Outline: A tag needs a name and a known category
    When I POST to /secured/tag with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    When I GET to /secured/tag
    Then response list size is 0
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
    Then response list size is 1
    And Tag table should have exactly 1 records in database

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
    Then response list size is 1
    And Tag table should have exactly 1 records in database

  Scenario: Listing tags returns tags from every category
    Given a tag named "Organic" exists
    And an expense tag named "Rent" exists
    When I GET to /secured/tag
    Then response list size is 2
    And response contains item with details:
      """json
      {"category": "EXPENSE", "tagName": "Rent"}
      """

  Scenario: Updating a tag that does not exist is rejected
    When I PUT to /secured/tag with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "tagName": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Tag table should have no records in database

  Scenario: An update without a name is rejected before the tag is looked up
    When I PUT to /secured/tag with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "tagName": "   "}
      """
    Then the response status should be 400
    And the response error message should be "A tag must have a name"
    And Tag table should have no records in database

  Scenario: Deleting a tag that does not exist changes nothing
    Given a tag named "Organic" exists
    When I DELETE to /secured/tag/00000000-0000-0000-0000-000000000001
    Then the response status should be 204
    When I GET to /secured/tag
    Then response list size is 1
    And Tag table should have exactly 1 records in database
