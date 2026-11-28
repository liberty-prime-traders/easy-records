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

  Scenario: A stock decrease empties the oldest layer first and leaves the newer one partly used
    Given stock of "Milk" is delivered by "Acme Dairy" in layers:
      | quantity | unit cost |
      | 5        | 5.00      |
      | 6        | 6.00      |
    And the location product named "Milk" should have "11" on hand
    When I POST to /secured/stock-adjustments with payload:
      """json
      {
        "locationProductId": "{{locationProductId:Milk}}",
        "category": "INTERNAL_USE",
        "quantity": 7,
        "unitId": "{{unitValue}}",
        "note": "Staff breakfast"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"category": "INTERNAL_USE", "direction": "DECREASE", "quantity": 7, "quantityText": "7 Pieces", "totalCost": 37}
      """
    And the location product named "Milk" should have "4" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {"storageBatchSize": 5, "storageQuantityRemaining": 0, "batchCost": 25.00, "costRemaining": 0.00},
        {"storageBatchSize": 6, "storageQuantityRemaining": 4, "batchCost": 36.00, "costRemaining": 24.00}
      ]
      """
    And StockMovement table should contain exactly these records:
      """json
      [
        {"movementType": "PURCHASE_RECEIVED", "storageMovedQuantity": 5, "storageRemainingQuantity": 5, "movedCost": 25.00},
        {"movementType": "PURCHASE_RECEIVED", "storageMovedQuantity": 6, "storageRemainingQuantity": 11, "movedCost": 36.00},
        {
          "movementType": "STOCK_ADJUSTMENT_DECREASE", "storageMovedQuantity": 5, "storageRemainingQuantity": 6,
          "movedCost": 25.00, "wholeLineQuantityText": "7 Pieces"
        },
        {
          "movementType": "STOCK_ADJUSTMENT_DECREASE", "storageMovedQuantity": 2, "storageRemainingQuantity": 4,
          "movedCost": 12.00, "wholeLineQuantityText": "7 Pieces"
        }
      ]
      """

  Scenario: A later decrease finishes the partly used layer before it opens the next one
    Given stock of "Milk" is delivered by "Acme Dairy" in layers:
      | quantity | unit cost |
      | 5        | 5.00      |
      | 6        | 6.00      |
    And the location product named "Milk" should have "11" on hand
    And I POST to /secured/stock-adjustments with payload:
      """json
      {
        "locationProductId": "{{locationProductId:Milk}}",
        "category": "INTERNAL_USE",
        "quantity": 3,
        "unitId": "{{unitValue}}",
        "note": "Staff breakfast"
      }
      """
    And the location product named "Milk" should have "8" on hand
    When I POST to /secured/stock-adjustments with payload:
      """json
      {
        "locationProductId": "{{locationProductId:Milk}}",
        "category": "INTERNAL_USE",
        "quantity": 4,
        "unitId": "{{unitValue}}",
        "note": "Tasting table"
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"quantity": 4, "quantityText": "4 Pieces", "totalCost": 22, "note": "Tasting table"}
      """
    And the location product named "Milk" should have "4" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {"storageBatchSize": 5, "storageQuantityRemaining": 0, "batchCost": 25.00, "costRemaining": 0.00},
        {"storageBatchSize": 6, "storageQuantityRemaining": 4, "batchCost": 36.00, "costRemaining": 24.00}
      ]
      """
    And StockMovement table should contain exactly these records:
      """json
      [
        {"movementType": "PURCHASE_RECEIVED", "storageMovedQuantity": 5, "storageRemainingQuantity": 5, "movedCost": 25.00},
        {"movementType": "PURCHASE_RECEIVED", "storageMovedQuantity": 6, "storageRemainingQuantity": 11, "movedCost": 36.00},
        {
          "movementType": "STOCK_ADJUSTMENT_DECREASE", "storageMovedQuantity": 3, "storageRemainingQuantity": 8,
          "movedCost": 15.00, "wholeLineQuantityText": "3 Pieces"
        },
        {
          "movementType": "STOCK_ADJUSTMENT_DECREASE", "storageMovedQuantity": 2, "storageRemainingQuantity": 6,
          "movedCost": 10.00, "wholeLineQuantityText": "4 Pieces"
        },
        {
          "movementType": "STOCK_ADJUSTMENT_DECREASE", "storageMovedQuantity": 2, "storageRemainingQuantity": 4,
          "movedCost": 12.00, "wholeLineQuantityText": "4 Pieces"
        }
      ]
      """

  Scenario: A stock decrease larger than what is on hand is refused and every layer stays whole
    Given stock of "Milk" is delivered by "Acme Dairy" in layers:
      | quantity | unit cost |
      | 5        | 5.00      |
      | 6        | 6.00      |
    And the location product named "Milk" should have "11" on hand
    When I POST to /secured/stock-adjustments with payload:
      """json
      {
        "locationProductId": "{{locationProductId:Milk}}",
        "category": "INTERNAL_USE",
        "quantity": 12,
        "unitId": "{{unitValue}}",
        "note": "Staff breakfast"
      }
      """
    Then the response status should be 400
    And the response error should contain "Milk. Available: 11 Pieces, Requested: 12 Pieces"
    And the location product named "Milk" should have "11" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {"storageBatchSize": 5, "storageQuantityRemaining": 5, "batchCost": 25.00, "costRemaining": 25.00},
        {"storageBatchSize": 6, "storageQuantityRemaining": 6, "batchCost": 36.00, "costRemaining": 36.00}
      ]
      """
    And StockMovement table should have exactly 2 records in database
    And StockAdjustment table should have no records in database
