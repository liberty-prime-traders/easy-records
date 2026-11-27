@consumes-from-kafka
Feature: Receiving after a unit's size was edited
  As a receiving clerk
  I want what is already stored to keep the size it was recorded with
  So that stock, its cost and its history stay true when a unit's size is corrected later

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And a unit group named "Beverages" with base unit "Bottle" coded "btl" exists
    And a unit named "Crate" coded "crate" equal to 12 of unit "btl" exists
    And a product named "Cola" with base unit "crate" exists
    And a supplier named "Acme Dairy" exists
    And the location product named "Cola" is available
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit  |
      | Cola    | 2        | 120.00    | crate |

  Scenario: A delivery after the edit takes the new size and the order line keeps the size it was ordered with
    Given I PUT to /secured/unitvalues with payload:
      """json
      {
        "id": "{{unitValue:crate}}",
        "name": "Crate",
        "code": "crate",
        "baseUnit": "{{unitValue:btl}}",
        "unitsOfBasePerUnit": 10
      }
      """
    And a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit  |
      | Cola    | 1        | 120.00    | crate |
    Then the location product named "Cola" should have "1" on hand
    And PurchaseLine should exist in database with id {{purchaseLineId:Cola}} and options:
      """json
      {"storageUnitsPerUserProvidedUnit": 12, "storageQuantityDelivered": 10}
      """
    And StockEntry table should contain exactly these records:
      """json
      [{"storageBatchSize": 10, "batchCost": 120.00, "unitCost": 12}]
      """
    And StockMovement table should contain exactly these records:
      """json
      [{"storageMovedQuantity": 10, "movedCost": 120.00, "storageUnitsPerUserProvidedUnit": 10}]
      """
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name           | type   | amount |
      | 001.001.003 | Inventory      | debit  | 120.00 |
      | 002.001.001 | Trade Payables | credit | 120.00 |

  Scenario: An edit between two deliveries leaves layers of different sizes and the books equal to the money paid
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit  |
      | Cola    | 1        | 120.00    | crate |
    And the location product named "Cola" should have "1" on hand
    And I PUT to /secured/unitvalues with payload:
      """json
      {
        "id": "{{unitValue:crate}}",
        "name": "Crate",
        "code": "crate",
        "baseUnit": "{{unitValue:btl}}",
        "unitsOfBasePerUnit": 10
      }
      """
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit  |
      | Cola    | 1        | 120.00    | crate |
    Then the location product named "Cola" should have "2.2" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {"storageBatchSize": 12, "batchCost": 120.00, "unitCost": 10},
        {"storageBatchSize": 10, "batchCost": 120.00, "unitCost": 12}
      ]
      """
    And the account balances should be:
      | account     | name           | balance |
      | 001.001.003 | Inventory      | 240.00  |
      | 002.001.001 | Trade Payables | 240.00  |

  Scenario: Stock history keeps each movement's own text and says where a unit's size has since changed
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit  |
      | Cola    | 1        | 120.00    | crate |
    And the location product named "Cola" should have "1" on hand
    And I PUT to /secured/unitvalues with payload:
      """json
      {
        "id": "{{unitValue:crate}}",
        "name": "Crate",
        "code": "crate",
        "baseUnit": "{{unitValue:btl}}",
        "unitsOfBasePerUnit": 10
      }
      """
    And a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit  |
      | Cola    | 1        | 120.00    | crate |
    And the location product named "Cola" should have "2.2" on hand
    When I GET to /secured/location-products/history with query parameters:
      | locationProductId | {{locationProductId:Cola}} |
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      [
        {
          "quantityMoved": "1 Crate",
          "quantityEntered": "entered 1 Crate",
          "newQuantity": "2 Crates 2 Bottles",
          "conversionDriftNote": null
        },
        {
          "quantityMoved": "1 Crate",
          "quantityEntered": "entered 1 Crate",
          "newQuantity": "1 Crate",
          "conversionDriftNote": "1 Crate was 12 Bottles"
        }
      ]
      """
    And StockMovement table should contain exactly these records:
      """json
      [
        {"storageMovedQuantity": 12, "storageUnitsPerUserProvidedUnit": 12},
        {"storageMovedQuantity": 10, "storageUnitsPerUserProvidedUnit": 10}
      ]
      """
