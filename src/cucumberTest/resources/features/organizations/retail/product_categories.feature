Feature: Product categories
  As an authenticated organization user
  I want to manage the categories that organize my product groups
  So that every group sits in a category with a unique name

  Scenario: Creating a category stores it and lists it
    When I POST to /secured/product-category with payload:
      """json
      {"categoryName": "Dairy", "description": "Milk and cheese"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"categoryName": "Dairy", "description": "Milk and cheese"}
      """
    When I GET to /secured/product-category
    Then response contains item with details:
      """json
      {"categoryName": "Dairy", "description": "Milk and cheese"}
      """
    And Product_Category should match example:
      """json
      {"categoryName": "Dairy", "description": "Milk and cheese"}
      """

  Scenario Outline: A category needs a name
    When I POST to /secured/product-category with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "A category must have a name"
    When I GET to /secured/product-category
    Then response list size is 0

    Examples:
      | payload                 |
      | {}                      |
      | {"categoryName": ""}    |
      | {"categoryName": "   "} |

  Scenario Outline: Category names are unique on their letters and digits alone
    Given a category named "Fresh Dairy" exists
    When I POST to /secured/product-category with payload:
      """json
      {"categoryName": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A category with the name <name> already exists."
    When I GET to /secured/product-category
    Then response list size is 1

    Examples:
      | name          |
      | fresh-dairy   |
      | FRESH DAIRY   |
      | Fresh_Dairy   |
      | freshdairy    |
      | Fresh  Dairy! |

  Scenario Outline: A category name needs a letter or digit
    When I POST to /secured/product-category with payload:
      """json
      {"categoryName": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A category name must contain at least one letter or digit"
    When I GET to /secured/product-category
    Then response list size is 0

    Examples:
      | name |
      | ***  |
      | ---  |
      | _ _  |
      | !?   |

  Scenario: Renaming a category frees its old name
    Given a category named "Dairy" exists
    When I PUT to /secured/product-category with payload:
      """json
      {"id": "#category", "categoryName": "Cold Dairy"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"id": "#category", "categoryName": "Cold Dairy"}
      """
    And Product_Category should exist in database with id #category and options:
      """json
      {"categoryName": "Cold Dairy"}
      """
    When I POST to /secured/product-category with payload:
      """json
      {"categoryName": "Dairy"}
      """
    Then the response status should be 200

  Scenario: An update that only carries a description keeps the name
    Given a category named "Dairy" exists
    When I PUT to /secured/product-category with payload:
      """json
      {"id": "#category", "description": "Milk and cheese"}
      """
    Then the response status should be 200
    And Product_Category should exist in database with id #category and options:
      """json
      {"categoryName": "Dairy", "description": "Milk and cheese"}
      """

  Scenario: A null description clears the category's description
    Given a category named "Dairy" exists
    And I PUT to /secured/product-category with payload:
      """json
      {"id": "#category", "description": "Temporary"}
      """
    When I PUT to /secured/product-category with payload:
      """json
      {"id": "#category", "description": null}
      """
    Then the response status should be 200
    And Product_Category should exist in database with id #category and options:
      """json
      {"categoryName": "Dairy", "description": null}
      """

  Scenario Outline: A category cannot be renamed to another category's name
    Given a category named "Dairy" exists
    And a category named "Bakery" exists
    When I PUT to /secured/product-category with payload:
      """json
      {"id": "#category", "categoryName": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A category with the name <name> already exists."
    And Product_Category should exist in database with id #category and options:
      """json
      {"categoryName": "Bakery"}
      """

    Examples:
      | name   |
      | dairy  |
      | DAIRY  |
      | Da-iry |
      | Dairy! |

  Scenario: An update cannot clear the category name
    Given a category named "Dairy" exists
    When I PUT to /secured/product-category with payload:
      """json
      {"id": "#category", "categoryName": null}
      """
    Then the response status should be 400
    And the response error message should be "A category must have a name"
    And Product_Category should exist in database with id #category and options:
      """json
      {"categoryName": "Dairy"}
      """

  Scenario: Updating a category that does not exist is rejected
    When I PUT to /secured/product-category with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "categoryName": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"

  Scenario: Deleting a category removes it
    Given a category exists
    When I DELETE to /secured/product-category/#category
    Then the response status should be 204
    When I GET to /secured/product-category
    Then response list size is 0
    And Product_Category should not exist in database with id #category

  Scenario: A category that holds a product group cannot be deleted
    Given a category exists
    And a product group exists
    When I DELETE to /secured/product-category/#category
    Then the response status should be 400
    And the response error message should be "A category with product groups cannot be deleted; move or delete those groups first"
    And Product_Category should exist in database with id #category
