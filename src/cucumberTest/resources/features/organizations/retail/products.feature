Feature: Organization products
  As an authenticated organization user
  I want to manage the products my organization sells
  So that every product sits in a group and is measured in a base unit

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected

  Scenario: Creating a product stores it with its group, category and base unit
    When I POST to /secured/products with payload:
      """json
      {
        "productName": "Whole Milk",
        "description": "One litre",
        "productGroupId": "{{productGroup}}",
        "baseUnitId": "{{unitValue}}"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"productName": "Whole Milk", "categoryName": "Grocery", "productGroupName": "Dairy", "baseUnit": "Piece"}
      """
    And Organization_Product should match example:
      """json
      {"productName": "Whole Milk", "productGroupId": "{{productGroup}}", "baseUnitId": "{{unitValue}}"}
      """

  Scenario Outline: A product needs a name, a group and a base unit
    When I POST to /secured/products with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    And Organization_Product table should have no records in database

    Examples:
      | payload | message |
      | {"productName":"  ","productGroupId":"{{productGroup}}","baseUnitId":"{{unitValue}}"} | A product must have a name |
      | {"productGroupId":"{{productGroup}}","baseUnitId":"{{unitValue}}"} | The request body could not be read |
      | {"productName":"Whole Milk","baseUnitId":"{{unitValue}}"} | The request body could not be read |
      | {"productName":"Whole Milk","productGroupId":"{{productGroup}}"} | The request body could not be read |

  Scenario Outline: A product must point at an existing group and base unit
    When I POST to /secured/products with payload:
      """json
      {"productName": "Whole Milk", "productGroupId": <productGroupId>, "baseUnitId": <baseUnitId>}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    And Organization_Product table should have no records in database

    Examples:
      | productGroupId | baseUnitId | message |
      | "00000000-0000-0000-0000-000000000001" | "{{unitValue}}" | The product group ID provided does not exist. |
      | "{{productGroup}}" | "00000000-0000-0000-0000-000000000001" | The base unit ID provided does not exist. |

  Scenario Outline: Product names are unique on their letters and digits alone
    Given a product named "Whole Milk" exists
    When I POST to /secured/products with payload:
      """json
      {"productName": "<name>", "productGroupId": "{{productGroup}}", "baseUnitId": "{{unitValue}}"}
      """
    Then the response status should be 400
    And the response error message should be "A product with the name <name> already exists."
    And Organization_Product table should have exactly 1 records in database

    Examples:
      | name         |
      | whole-milk   |
      | WHOLE MILK   |
      | Whole_Milk   |
      | wholemilk    |
      | Whole  Milk! |

  Scenario Outline: A product name needs a letter or digit
    When I POST to /secured/products with payload:
      """json
      {"productName": "<name>", "productGroupId": "{{productGroup}}", "baseUnitId": "{{unitValue}}"}
      """
    Then the response status should be 400
    And the response error message should be "A product name must contain at least one letter or digit"
    And Organization_Product table should have no records in database

    Examples:
      | name |
      | ***  |
      | ---  |
      | _ _  |
      | !?   |

  Scenario: Product names are unique across groups
    Given a product named "Whole Milk" exists
    And a product group named "Other Group" exists
    When I POST to /secured/products with payload:
      """json
      {"productName": "Whole Milk", "productGroupId": "{{productGroup}}", "baseUnitId": "{{unitValue}}"}
      """
    Then the response status should be 400
    And the response error message should be "A product with the name Whole Milk already exists."
    And Organization_Product table should have exactly 1 records in database

  Scenario: Renaming a product frees its old name
    Given a product named "Whole Milk" exists
    When I PUT to /secured/products with payload:
      """json
      {"id": "{{product}}", "productName": "Skim Milk"}
      """
    Then the response status should be 200
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"productName": "Skim Milk", "nameKey": "skimmilk"}
      """
    When I POST to /secured/products with payload:
      """json
      {"productName": "Whole Milk", "productGroupId": "{{productGroup}}", "baseUnitId": "{{unitValue}}"}
      """
    Then the response status should be 200
    And Organization_Product table should contain exactly these records:
      """json
      [{"productName": "Skim Milk", "nameKey": "skimmilk"}, {"productName": "Whole Milk", "nameKey": "wholemilk"}]
      """

  Scenario: An update that only carries a description keeps the name and group
    Given a product named "Whole Milk" exists
    When I PUT to /secured/products with payload:
      """json
      {"id": "{{product}}", "description": "One litre"}
      """
    Then the response status should be 200
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"productName": "Whole Milk", "description": "One litre", "productGroupId": "{{productGroup}}"}
      """

  Scenario: A null description clears the product's description
    Given a product named "Whole Milk" exists
    And I PUT to /secured/products with payload:
      """json
      {"id": "{{product}}", "description": "Temporary"}
      """
    When I PUT to /secured/products with payload:
      """json
      {"id": "{{product}}", "description": null}
      """
    Then the response status should be 200
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"productName": "Whole Milk", "description": null}
      """

  Scenario: A product can move to another group
    Given a product named "Whole Milk" exists
    And a product group named "Other Group" exists
    When I PUT to /secured/products with payload:
      """json
      {"id": "{{product}}", "productGroupId": "{{productGroup}}"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"productName": "Whole Milk", "productGroupName": "Other Group"}
      """
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"productGroupId": "{{productGroup}}"}
      """

  Scenario Outline: A product cannot be moved to a missing or empty group
    Given a product named "Whole Milk" exists
    When I PUT to /secured/products with payload:
      """json
      {"id": "{{product}}", "productGroupId": <productGroupId>}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"productGroupId": "{{productGroup}}"}
      """

    Examples:
      | productGroupId                         | message                                             |
      | "00000000-0000-0000-0000-000000000001" | The product group ID provided does not exist.       |
      | null                                   | If a product group is provided, it cannot be empty. |

  Scenario Outline: A product cannot be renamed to another product's name
    Given a product named "Whole Milk" exists
    And a product named "Skim Milk" exists
    When I PUT to /secured/products with payload:
      """json
      {"id": "{{product}}", "productName": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A product with the name <name> already exists."
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"productName": "Skim Milk"}
      """

    Examples:
      | name         |
      | whole milk   |
      | WHOLE MILK   |
      | Whole-Milk   |
      | Whole  Milk! |

  Scenario: The base unit is fixed when the product is created
    Given a product named "Whole Milk" exists
    And the system unit "kg" is selected
    When I PUT to /secured/products with payload:
      """json
      {"id": "{{product}}", "description": "Changed", "baseUnitId": "{{unitValue}}"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"productName": "Whole Milk", "baseUnit": "Piece", "baseUnitId": "{{unitValue->0}}"}
      """
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"description": "Changed"}
      """
    And Organization_Product should match example:
      """json
      {"productName": "Whole Milk", "baseUnitId": "{{unitValue->0}}"}
      """

  Scenario: Updating a product that does not exist is rejected
    When I PUT to /secured/products with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "productName": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Organization_Product table should have no records in database

  Scenario: A product can be discontinued and reactivated
    Given a product named "Whole Milk" exists
    When I PUT to /secured/products/{{product}}/deactivate
    Then the response status should be 200
    And response contains details:
      """json
      {"productName": "Whole Milk", "status": "DISCONTINUED"}
      """
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"status": "DISCONTINUED"}
      """
    When I PUT to /secured/products/{{product}}/reactivate
    Then the response status should be 200
    And response contains details:
      """json
      {"productName": "Whole Milk", "status": "ACTIVE"}
      """
    And Organization_Product should exist in database with id {{product}} and options:
      """json
      {"status": "ACTIVE"}
      """

  Scenario Outline: Changing the status of a product that does not exist is rejected
    When I PUT to /secured/products/00000000-0000-0000-0000-000000000001/<action>
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Organization_Product table should have no records in database

    Examples:
      | action     |
      | deactivate |
      | reactivate |

  @publishes-to-kafka
  Scenario: Creating a product publishes a catalog event for it
    When I POST to /secured/products with payload:
      """json
      {"productName": "Whole Milk", "productGroupId": "{{productGroup}}", "baseUnitId": "{{unitValue}}"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"productName": "Whole Milk", "categoryName": "Grocery", "productGroupName": "Dairy", "baseUnit": "Piece"}
      """
    And a catalog event should be published for table "PRODUCT"
    And the catalog event should reference the created resource
    And Organization_Product should match example:
      """json
      {"productName": "Whole Milk"}
      """
