@consumes-from-kafka
Feature: Stock delivered in known layers
  As a test author
  I want a step that leaves a product with stock in layers of known size and cost
  So that sale scenarios start from FIFO layers I can read off the feature

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And a product named "Milk" exists
    And a product named "Bread" exists
    And a supplier named "Acme Dairy" exists
    And the location product named "Milk" is available
    And the location product named "Bread" is available

  Scenario: Each row becomes its own untouched layer at its own cost
    Given stock of "Milk" is delivered by "Acme Dairy" in layers:
      | quantity | unit cost |
      | 10       | 5.00      |
      | 20       | 6.00      |
    Then the location product named "Milk" should have "30" on hand
    And StockEntry table should contain exactly these records:
      """json
      [
        {
          "storageBatchSize": 10,
          "storageQuantityRemaining": 10,
          "batchCost": 50.00,
          "costRemaining": 50.00,
          "unitCost": 5,
          "status": "ACTIVE"
        },
        {
          "storageBatchSize": 20,
          "storageQuantityRemaining": 20,
          "batchCost": 120.00,
          "costRemaining": 120.00,
          "unitCost": 6,
          "status": "ACTIVE"
        }
      ]
      """
    And the account balances should be:
      | account     | name             | balance |
      | 001.001.003 | Inventory        | 170.00  |
      | 001.001     | Current Assets   | 170.00  |
      | 001         | Assets           | 170.00  |
      | 002.001.001 | Trade Payables   | 170.00  |
      | 002.001     | Accounts Payable | 170.00  |
      | 002         | Liabilities      | 170.00  |
    And no other account should have a balance
    And every parent account should equal the sum of its children

  Scenario: Two products can be stocked one after the other
    Given stock of "Milk" is delivered by "Acme Dairy" in layers:
      | quantity | unit cost |
      | 10       | 5.00      |
    And stock of "Bread" is delivered by "Acme Dairy" in layers:
      | quantity | unit cost |
      | 12       | 2.50      |
    Then the location product named "Milk" should have "10" on hand
    And the location product named "Bread" should have "12" on hand
    And StockEntry table should have exactly 2 records in database
    And PurchaseDelivery table should have exactly 2 records in database
