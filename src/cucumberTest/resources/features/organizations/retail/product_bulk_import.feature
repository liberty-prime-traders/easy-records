Feature: Bulk import of categories, product groups and products
  As an authenticated organization user
  I want to load a catalog in one request
  So that categories, groups and products are saved together or not at all

  Scenario: Categories, groups and products are saved together and linked by name
    When I POST to /secured/products/bulk with payload:
      """json
      {
        "categories": [
          {"name": "Grocery", "description": "Food and drink"},
          {"name": "Household"}
        ],
        "productGroups": [
          {"name": "Dairy", "categoryName": "Grocery"},
          {"name": "Cleaning", "categoryName": "Household", "description": "Soaps and sprays"}
        ],
        "products": [
          {"name": "Whole Milk", "description": "One litre", "groupName": "Dairy", "unitCode": "pc"},
          {"name": "Butter", "groupName": "Dairy", "unitCode": "pc"},
          {"name": "Dish Soap", "groupName": "Cleaning", "unitCode": "pc"}
        ]
      }
      """
    Then the response status should be 200
    And Product_Category table should have exactly 2 records in database
    And Product_Group table should have exactly 2 records in database
    And Organization_Product table should have exactly 3 records in database
    And Product_Category should match example:
      """json
      {"categoryName": "Grocery", "description": "Food and drink"}
      """
    And Organization_Product should match example:
      """json
      {"productName": "Whole Milk", "nameKey": "wholemilk", "description": "One litre"}
      """
    When I GET to /secured/product-groups
    Then response contains item with details:
      """json
      {"groupName": "Cleaning", "categoryName": "Household", "description": "Soaps and sprays"}
      """
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {}}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "contents": [
          {
            "productName": "Whole Milk",
            "description": "One litre",
            "productGroupName": "Dairy",
            "categoryName": "Grocery",
            "baseUnit": "Piece",
            "status": "ACTIVE"
          },
          {"productName": "Butter", "productGroupName": "Dairy", "categoryName": "Grocery"},
          {"productName": "Dish Soap", "productGroupName": "Cleaning", "categoryName": "Household"}
        ]
      }
      """

  Scenario Outline: A payload that restates an existing category or group reuses it, however it is spelled
    Given a category named "Fresh Dairy" exists
    And a product group named "Cold Cuts" exists
    When I POST to /secured/products/bulk with payload:
      """json
      {
        "categories": [{"name": "<categorySpelling>"}],
        "productGroups": [{"name": "<groupSpelling>", "categoryName": "<categorySpelling>"}],
        "products": [{"name": "Ham", "groupName": "<groupSpelling>", "unitCode": "pc"}]
      }
      """
    Then the response status should be 200
    And Product_Category table should have exactly 1 records in database
    And Product_Category should match example:
      """json
      {"categoryName": "Fresh Dairy"}
      """
    And Product_Group table should have exactly 1 records in database
    And Product_Group should match example:
      """json
      {"groupName": "Cold Cuts"}
      """
    When I POST to /secured/products/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 50, "parameters": {}}
      """
    Then response contains details:
      """json
      {"contents": [{"productName": "Ham", "productGroupName": "Cold Cuts"}]}
      """

    Examples:
      | categorySpelling | groupSpelling |
      | fresh-dairy      | cold-cuts     |
      | FRESH DAIRY      | COLD CUTS     |
      | Fresh_Dairy      | Cold_Cuts     |
      | freshdairy       | coldcuts      |
      | Fresh  Dairy!    | Cold  Cuts!   |

  Scenario: A rejected payload saves nothing and reports every problem
    Given a category named "Staples" exists
    And a product group named "Pantry Items" exists
    And the system unit "pc" is selected
    And a product named "Whole Milk" exists
    When I POST to /secured/products/bulk with payload:
      """json
      {
        "categories": [
          {"name": "Grocery"},
          {"name": "grocery!"},
          {"name": "---"}
        ],
        "productGroups": [
          {"name": "Dairy"},
          {"name": "Bakery", "categoryName": "Nowhere"},
          {"name": "Pantry", "categoryName": "Grocery"}
        ],
        "products": [
          {"name": "Cheese", "groupName": "Pantry", "unitCode": "nope"},
          {"name": "cheese", "groupName": "Pantry", "unitCode": "pc"},
          {"name": "whole-milk", "groupName": "Pantry", "unitCode": "pc"},
          {"name": "Bread", "groupName": "Missing Group", "unitCode": "pc"}
        ]
      }
      """
    Then the response status should be 400
    And the response error message should be "Bulk product import validation failed"
    And response contains details:
      """json
      {
        "body": [
          "Category at position 2: duplicates category name 'grocery!' at position 1",
          "Category at position 3: name must contain at least one letter or digit",
          "Product group at position 1: categoryName is required",
          "Product group at position 2: categoryName 'Nowhere' does not match any category in this payload or in the organization",
          "Product at position 1: unitCode 'nope' does not match any existing unit value",
          "Product at position 2: duplicates product name 'cheese' at position 1",
          "Product at position 3: a product with the name 'whole-milk' already exists",
          "Product at position 4: groupName 'Missing Group' does not match any product group in this payload or in the organization"
        ]
      }
      """
    And Product_Category table should have exactly 1 records in database
    And Product_Group table should have exactly 1 records in database
    And Organization_Product table should have exactly 1 records in database

  Scenario Outline: A product row needs a name, a group and a unit code
    When I POST to /secured/products/bulk with payload:
      """json
      {
        "categories": [{"name": "Grocery"}],
        "productGroups": [{"name": "Dairy", "categoryName": "Grocery"}],
        "products": [<product>]
      }
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["<error>"]}
      """
    And Product_Category table should have no records in database
    And Organization_Product table should have no records in database

    Examples:
      | product                                            | error                                                                 |
      | {"groupName":"Dairy","unitCode":"pc"}              | Product at position 1: name is required                               |
      | {"name":"  ","groupName":"Dairy","unitCode":"pc"}  | Product at position 1: name is required                               |
      | {"name":"Milk","unitCode":"pc"}                    | Product at position 1: groupName is required                          |
      | {"name":"Milk","groupName":"Dairy"}                | Product at position 1: unitCode is required                           |
      | {"name":"Milk","groupName":"Dairy","unitCode":""}  | Product at position 1: unitCode is required                           |
      | {"name":"!!!","groupName":"Dairy","unitCode":"pc"} | Product at position 1: name must contain at least one letter or digit |

  Scenario Outline: Product names in one payload cannot match on their letters and digits
    When I POST to /secured/products/bulk with payload:
      """json
      {
        "categories": [{"name": "Grocery"}],
        "productGroups": [{"name": "Dairy", "categoryName": "Grocery"}],
        "products": [
          {"name": "Whole Milk", "groupName": "Dairy", "unitCode": "pc"},
          {"name": "<spelling>", "groupName": "Dairy", "unitCode": "pc"}
        ]
      }
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Product at position 2: duplicates product name '<spelling>' at position 1"]}
      """
    And Organization_Product table should have no records in database

    Examples:
      | spelling      |
      | whole milk    |
      | Whole-Milk    |
      | WHOLE_MILK    |
      | wholemilk     |
      | Whole  Milk!! |

  Scenario Outline: A payload product cannot take the name of an existing product
    Given a category named "Staples" exists
    And a product group named "Pantry Items" exists
    And the system unit "pc" is selected
    And a product named "Whole Milk" exists
    When I POST to /secured/products/bulk with payload:
      """json
      {
        "products": [{"name": "<spelling>", "groupName": "Pantry Items", "unitCode": "pc"}]
      }
      """
    Then the response status should be 400
    And response contains details:
      """json
      {"body": ["Product at position 1: a product with the name '<spelling>' already exists"]}
      """
    And Organization_Product table should have exactly 1 records in database

    Examples:
      | spelling   |
      | whole-milk |
      | WHOLE MILK |
      | wholemilk  |

  Scenario Outline: Category and group names in one payload cannot match on their letters and digits
    When I POST to /secured/products/bulk with payload:
      """json
      {
        "categories": [{"name": "Fresh Dairy"}, {"name": "<spelling>"}],
        "productGroups": [
          {
            "name": "Cold Cuts",
            "categoryName": "Fresh Dairy"
          },
          {
            "name": "<groupSpelling>",
            "categoryName": "Fresh Dairy"
          }
        ]
      }
      """
    Then the response status should be 400
    And response contains details:
      """json
      {
        "body": [
          "Category at position 2: duplicates category name '<spelling>' at position 1",
          "Product group at position 2: duplicates product group name '<groupSpelling>' at position 1"
        ]
      }
      """
    And Product_Category table should have no records in database
    And Product_Group table should have no records in database

    Examples:
      | spelling    | groupSpelling |
      | fresh-dairy | cold-cuts     |
      | FRESH DAIRY | COLD CUTS     |
      | Fresh_Dairy | Cold_Cuts     |
      | freshdairy  | coldcuts      |

  Scenario: A payload with nothing in it is accepted and saves nothing
    When I POST to /secured/products/bulk with payload:
      """json
      {}
      """
    Then the response status should be 200
    And Product_Category table should have no records in database
    And Organization_Product table should have no records in database
