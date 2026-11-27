@consumes-from-kafka
Feature: Stock layers created by a delivery
  As a stock controller
  I want each delivered line to open its own cost layer and leave a movement behind
  So that stock on hand, its cost and its history follow what the supplier delivered

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And a product named "Milk" exists
    And a product named "Bread" exists
    And a supplier named "Acme Dairy" exists
    And the location product named "Milk" is available
    And the location product named "Bread" is available
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost |
      | Milk    | 10       | 5.00      |
      | Bread   | 12       | 2.50      |

  Scenario: A delivered line opens one layer at its delivered cost and one received movement
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    Then the location product named "Milk" should have "4" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {
          "sourceType": "PURCHASE", "documentReference": "{{deliveryReference}}",
          "storageBatchSize": 4, "storageQuantityRemaining": 4,
          "batchCost": 20.00, "costRemaining": 20.00, "unitCost": 5, "status": "ACTIVE"
        }
      ]
      """
    And StockMovement table should contain exactly these records:
      """json
      [
        {
          "movementType": "PURCHASE_RECEIVED", "documentReference": "{{deliveryReference}}",
          "storageMovedQuantity": 4, "storageRemainingQuantity": 4, "movedCost": 20.00,
          "wholeLineQuantityText": "4 Pieces", "storageMovedBreakdownText": "4 Pieces",
          "storageRemainingBreakdownText": "4 Pieces", "context": "Purchase {{purchaseReference}}"
        }
      ]
      """

  Scenario: Each delivery opens its own layers and the movement remembers the balance after it
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 6        | 5.00      |
      | Bread   | 12       | 2.50      |
    Then the location product named "Milk" should have "10" on hand
    And the location product named "Bread" should have "12" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {"storageBatchSize": 4, "storageQuantityRemaining": 4, "batchCost": 20.00, "unitCost": 5},
        {"storageBatchSize": 6, "storageQuantityRemaining": 6, "batchCost": 30.00, "unitCost": 5},
        {"storageBatchSize": 12, "storageQuantityRemaining": 12, "batchCost": 30.00, "unitCost": 2.5}
      ]
      """
    And StockMovement table should contain exactly these records:
      """json
      [
        {
          "movementType": "PURCHASE_RECEIVED", "storageMovedQuantity": 4, "storageRemainingQuantity": 4,
          "storageRemainingBreakdownText": "4 Pieces"
        },
        {
          "movementType": "PURCHASE_RECEIVED", "storageMovedQuantity": 6, "storageRemainingQuantity": 10,
          "storageRemainingBreakdownText": "10 Pieces"
        },
        {
          "movementType": "PURCHASE_RECEIVED", "storageMovedQuantity": 12, "storageRemainingQuantity": 12,
          "storageRemainingBreakdownText": "12 Pieces"
        }
      ]
      """

  Scenario: The layer takes the delivered cost, not the order cost
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 10       | 6.00      |
    Then the location product named "Milk" should have "10" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {"storageBatchSize": 10, "batchCost": 60.00, "costRemaining": 60.00, "unitCost": 6}
      ]
      """
    And StockMovement table should contain exactly these records:
      """json
      [
        {"storageMovedQuantity": 10, "movedCost": 60.00, "wholeLineQuantityText": "10 Pieces"}
      ]
      """

  Scenario: Deliveries at different costs keep separate layers each at its own cost
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    And the location product named "Milk" should have "4" on hand
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 6        | 6.00      |
    Then the location product named "Milk" should have "10" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {"storageBatchSize": 4, "batchCost": 20.00, "unitCost": 5, "status": "ACTIVE"},
        {"storageBatchSize": 6, "batchCost": 36.00, "unitCost": 6, "status": "ACTIVE"}
      ]
      """
