@consumes-from-kafka
Feature: Receiving in a different unit
  As a receiving clerk
  I want to receive in dozens or pieces whichever unit the order used
  So that stock, its cost and the books follow what arrived in storage units

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And the system unit "dz" is selected
    And a product named "Eggs" with base unit "pc" exists
    And a supplier named "Acme Dairy" exists
    And the location product named "Eggs" is available
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 2        | 60.00     | dz   |

  Scenario: One dozen of a two dozen order is twelve pieces on one layer at the dozen price
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 1        | 60.00     | dz   |
    Then the location product named "Eggs" should have "12" on hand
    And PurchaseLine should exist in database with id {{purchaseLineId:Eggs}} and options:
      """json
      {"storageQuantityDelivered": 12}
      """
    And StockEntry table should contain exactly these records:
      """json
      [
        {
          "storageBatchSize": 12,
          "storageQuantityRemaining": 12,
          "batchCost": 60.00,
          "unitCost": 5
        }
      ]
      """
    And StockMovement table should contain exactly these records:
      """json
      [
        {
          "movementType": "PURCHASE_RECEIVED",
          "storageMovedQuantity": 12,
          "storageRemainingQuantity": 12,
          "movedCost": 60.00,
          "unitId": "{{unitValue:dz}}",
          "storageUnitsPerUserProvidedUnit": 12
        }
      ]
      """
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name           | type   | amount |
      | 001.001.003 | Inventory      | debit  | 60.00  |
      | 002.001.001 | Trade Payables | credit | 60.00  |

  Scenario: Receiving in pieces what was ordered in dozens stores the same twelve pieces
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 12       | 5.00      | pc   |
    Then the location product named "Eggs" should have "12" on hand
    And PurchaseLine should exist in database with id {{purchaseLineId:Eggs}} and options:
      """json
      {"storageQuantityDelivered": 12}
      """
    And StockEntry table should contain exactly these records:
      """json
      [
        {
          "storageBatchSize": 12,
          "batchCost": 60.00,
          "unitCost": 5
        }
      ]
      """
    And StockMovement table should contain exactly these records:
      """json
      [
        {
          "storageMovedQuantity": 12,
          "unitId": "{{unitValue:pc}}",
          "storageUnitsPerUserProvidedUnit": 1
        }
      ]
      """
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name           | type   | amount |
      | 001.001.003 | Inventory      | debit  | 60.00  |
      | 002.001.001 | Trade Payables | credit | 60.00  |

  Scenario: A dozen that does not divide into a whole price per piece keeps its exact batch cost
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 1        | 10.00     | dz   |
    Then the location product named "Eggs" should have "12" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {
          "storageBatchSize": 12,
          "batchCost": 10.00,
          "costRemaining": 10.00,
          "unitCost": 0.8333333333
        }
      ]
      """
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name           | type   | amount |
      | 001.001.003 | Inventory      | debit  | 10.00  |
      | 002.001.001 | Trade Payables | credit | 10.00  |
    And the ledger entries for "{{deliveryReference}}" should balance
