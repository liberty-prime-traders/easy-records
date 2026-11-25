Feature: Product groups
  As an authenticated organization user
  I want to manage the product groups inside my categories
  So that every product sits in a group with a unique name

  Background:
    Given a category exists

  Scenario: Creating a group stores it under its category
    When I POST to /secured/product-groups with payload:
      """json
      {"groupName": "Milk", "description": "Fresh milk", "categoryId": "#category"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"groupName": "Milk", "description": "Fresh milk", "categoryId": "#category", "categoryName": "Test Category"}
      """
    When I GET to /secured/product-groups
    Then response contains item with details:
      """json
      {"groupName": "Milk", "categoryName": "Test Category"}
      """
    And Product_Group should match example:
      """json
      {"groupName": "Milk", "description": "Fresh milk", "categoryId": "#category"}
      """

  Scenario Outline: A group needs a name and a category
    When I POST to /secured/product-groups with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    When I GET to /secured/product-groups
    Then response list size is 0

    Examples:
      | payload                                         | message                            |
      | {"groupName": "", "categoryId": "#category"}    | A product group must have a name   |
      | {"groupName": "   ", "categoryId": "#category"} | A product group must have a name   |
      | {"categoryId": "#category"}                     | The request body could not be read |
      | {"groupName": "Milk"}                           | The request body could not be read |

  Scenario: A group cannot point at a category that does not exist
    When I POST to /secured/product-groups with payload:
      """json
      {"groupName": "Milk", "categoryId": "00000000-0000-0000-0000-000000000001"}
      """
    Then the response status should be 400
    And the response error message should be "The category ID provided does not exist."
    When I GET to /secured/product-groups
    Then response list size is 0

  Scenario Outline: Group names are unique on their letters and digits alone
    Given a product group named "Fresh Milk" exists
    When I POST to /secured/product-groups with payload:
      """json
      {"groupName": "<name>", "categoryId": "#category"}
      """
    Then the response status should be 400
    And the response error message should be "A product group with the name <name> already exists."
    When I GET to /secured/product-groups
    Then response list size is 1

    Examples:
      | name         |
      | fresh-milk   |
      | FRESH MILK   |
      | Fresh_Milk   |
      | freshmilk    |
      | Fresh  Milk! |

  Scenario Outline: A group name needs a letter or digit
    When I POST to /secured/product-groups with payload:
      """json
      {"groupName": "<name>", "categoryId": "#category"}
      """
    Then the response status should be 400
    And the response error message should be "A product group name must contain at least one letter or digit"
    When I GET to /secured/product-groups
    Then response list size is 0

    Examples:
      | name |
      | ***  |
      | ---  |
      | _ _  |
      | !?   |

  Scenario: Group names are unique across categories
    Given a product group named "Milk" exists
    And a category named "Other" exists
    When I POST to /secured/product-groups with payload:
      """json
      {"groupName": "Milk", "categoryId": "#category"}
      """
    Then the response status should be 400
    And the response error message should be "A product group with the name Milk already exists."

  Scenario: Renaming a group frees its old name
    Given a product group named "Milk" exists
    When I PUT to /secured/product-groups with payload:
      """json
      {"id": "#productGroup", "groupName": "Cold Milk"}
      """
    Then the response status should be 200
    And Product_Group should exist in database with id #productGroup and options:
      """json
      {"groupName": "Cold Milk"}
      """
    When I POST to /secured/product-groups with payload:
      """json
      {"groupName": "Milk", "categoryId": "#category"}
      """
    Then the response status should be 200

  Scenario: An update that only carries a description keeps the name and category
    Given a product group named "Milk" exists
    When I PUT to /secured/product-groups with payload:
      """json
      {"id": "#productGroup", "description": "Fresh milk"}
      """
    Then the response status should be 200
    And Product_Group should exist in database with id #productGroup and options:
      """json
      {"groupName": "Milk", "description": "Fresh milk", "categoryId": "#category"}
      """

  Scenario: A null description clears the group's description
    Given a product group named "Milk" exists
    And I PUT to /secured/product-groups with payload:
      """json
      {"id": "#productGroup", "description": "Temporary"}
      """
    When I PUT to /secured/product-groups with payload:
      """json
      {"id": "#productGroup", "description": null}
      """
    Then the response status should be 200
    And Product_Group should exist in database with id #productGroup and options:
      """json
      {"groupName": "Milk", "description": null}
      """

  Scenario: A group can move to another category
    Given a product group named "Milk" exists
    And a category named "Other" exists
    When I PUT to /secured/product-groups with payload:
      """json
      {"id": "#productGroup", "categoryId": "#category"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"groupName": "Milk", "categoryName": "Other"}
      """
    And Product_Group should exist in database with id #productGroup and options:
      """json
      {"categoryId": "#category"}
      """

  Scenario Outline: A group cannot be moved to a missing or empty category
    Given a product group named "Milk" exists
    When I PUT to /secured/product-groups with payload:
      """json
      {"id": "#productGroup", "categoryId": <categoryId>}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    And Product_Group should exist in database with id #productGroup and options:
      """json
      {"categoryId": "#category"}
      """

    Examples:
      | categoryId                             | message                                        |
      | "00000000-0000-0000-0000-000000000001" | The category ID provided does not exist.       |
      | null                                   | If a category is provided, it cannot be empty. |

  Scenario Outline: A group cannot be renamed to another group's name
    Given a product group named "Milk" exists
    And a product group named "Cheese" exists
    When I PUT to /secured/product-groups with payload:
      """json
      {"id": "#productGroup", "groupName": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A product group with the name <name> already exists."
    And Product_Group should exist in database with id #productGroup and options:
      """json
      {"groupName": "Cheese"}
      """

    Examples:
      | name  |
      | milk  |
      | MILK  |
      | Mi-lk |
      | Milk! |

  Scenario: Updating a group that does not exist is rejected
    When I PUT to /secured/product-groups with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "groupName": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"

  Scenario: Deleting a group removes it
    Given a product group exists
    When I DELETE to /secured/product-groups/#productGroup
    Then the response status should be 204
    When I GET to /secured/product-groups
    Then response list size is 0
    And Product_Group should not exist in database with id #productGroup

  Scenario: A group that holds a product cannot be deleted
    Given a product group exists
    And the system unit "pc" is selected
    And a product named "Whole Milk" exists
    When I DELETE to /secured/product-groups/#productGroup
    Then the response status should be 400
    And the response error message should be "A product group with products cannot be deleted; move those products to another group first"
    And Product_Group should exist in database with id #productGroup
