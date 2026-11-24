Feature: Catalog Sync
  As a system
  I want changes to organization products to reach the location catalogs
  So that location inventory reflects the current catalog

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected

  @consumes-from-kafka
  Scenario: A created product appears in the location catalog with its group and unit
    When I POST to /secured/products with payload:
      """json
      {"productName": "Whole Milk", "description": "One litre", "productGroupId": "#productGroup", "baseUnitId": "#unitValue"}
      """
    Then the response status should be 200
    And the location catalog should contain a product matching:
      """json
      {
        "productName": "Whole Milk",
        "description": "One litre",
        "productGroupName": "Dairy",
        "baseUnit": "Piece",
        "status": "ACTIVE"
      }
      """
    And the location catalog should hold 1 product

  @consumes-from-kafka
  Scenario: Renaming a product updates its location record instead of adding another
    Given a product named "Whole Milk" exists
    When I PUT to /secured/products with payload:
      """json
      {"id": "#product", "productName": "Skim Milk", "description": "Fat free"}
      """
    Then the response status should be 200
    And the location catalog should contain a product matching:
      """json
      {"orgProductId": "#product", "productName": "Skim Milk", "description": "Fat free", "productGroupName": "Dairy"}
      """
    And the location catalog should hold 1 product

  @consumes-from-kafka
  Scenario: Moving a product to another group updates its group name in the location catalog
    Given a product named "Whole Milk" exists
    And a product group named "Beverages" exists
    When I PUT to /secured/products with payload:
      """json
      {"id": "#product", "productGroupId": "#productGroup"}
      """
    Then the response status should be 200
    And the location catalog should contain a product matching:
      """json
      {"orgProductId": "#product", "productName": "Whole Milk", "productGroupName": "Beverages"}
      """

  @consumes-from-kafka
  Scenario: Discontinuing a product discontinues it in the location catalog
    Given a product named "Whole Milk" exists
    When I PUT to /secured/products/#product/deactivate
    Then the response status should be 200
    And the location catalog should contain a product matching:
      """json
      {"orgProductId": "#product", "productName": "Whole Milk", "status": "DISCONTINUED"}
      """

  @consumes-from-kafka
  Scenario: A product created in the organization reaches every location catalog
    Given a location named "Second Shop" exists
    When I POST to /secured/products with payload:
      """json
      {"productName": "Whole Milk", "productGroupId": "#productGroup", "baseUnitId": "#unitValue"}
      """
    Then the response status should be 200
    And the location catalog should contain a product matching:
      """json
      {"productName": "Whole Milk", "productGroupName": "Dairy"}
      """
    And the catalog of location "Second Shop" should contain a product matching:
      """json
      {"productName": "Whole Milk", "productGroupName": "Dairy"}
      """
