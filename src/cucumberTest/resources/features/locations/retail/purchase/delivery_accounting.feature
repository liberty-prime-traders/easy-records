@consumes-from-kafka
Feature: Books posted by a delivery
  As an accountant
  I want each delivery to put the received goods in inventory and the amount owed in trade payables
  So that what we hold and what we owe the supplier always agree with what was delivered

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

  Scenario: A delivery debits inventory, credits trade payables and raises what the supplier is owed
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    Then the ledger group for "{{deliveryReference}}" should have source type "PURCHASE_DELIVERY"
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name             | type   | amount |
      | 001.001.003 | Inventory        | debit  | 20.00  |
      | 002.001.001 | Trade Payables   | credit | 20.00  |
    And the ledger entries for "{{deliveryReference}}" should balance
    And the subledger entries for "{{deliveryReference}}" should be exactly:
      | contact                          | payable | receivable | running payable | running receivable |
      | {{supplierReference:Acme Dairy}} | 20.00   | 0.00       | 20.00           | 0.00               |
    And the account balances should be:
      | account     | name             | balance |
      | 001.001.003 | Inventory        | 20.00   |
      | 001.001     | Current Assets   | 20.00   |
      | 001         | Assets           | 20.00   |
      | 002.001.001 | Trade Payables   | 20.00   |
      | 002.001     | Accounts Payable | 20.00   |
      | 002         | Liabilities      | 20.00   |
    And no other account should have a balance
    And every parent account should equal the sum of its children

  Scenario: A second delivery adds its own group and the supplier's running payable accumulates
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name             | type   | amount |
      | 001.001.003 | Inventory        | debit  | 20.00  |
      | 002.001.001 | Trade Payables   | credit | 20.00  |
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 6        | 5.00      |
      | Bread   | 12       | 2.50      |
    Then the ledger group for "{{deliveryReference}}" should have source type "PURCHASE_DELIVERY"
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name             | type   | amount |
      | 001.001.003 | Inventory        | debit  | 60.00  |
      | 002.001.001 | Trade Payables   | credit | 60.00  |
    And the subledger entries for "{{deliveryReference}}" should be exactly:
      | contact                          | payable | receivable | running payable | running receivable |
      | {{supplierReference:Acme Dairy}} | 60.00   | 0.00       | 80.00           | 0.00               |
    And the account balances should be:
      | account     | name             | balance |
      | 001.001.003 | Inventory        | 80.00   |
      | 001.001     | Current Assets   | 80.00   |
      | 001         | Assets           | 80.00   |
      | 002.001.001 | Trade Payables   | 80.00   |
      | 002.001     | Accounts Payable | 80.00   |
      | 002         | Liabilities      | 80.00   |
    And no other account should have a balance
    And every parent account should equal the sum of its children
    And LedgerEntryGroup table should have exactly 2 records in database

  Scenario: The delivered cost, not the order cost, is what is booked
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 10       | 6.00      |
    Then the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name             | type   | amount |
      | 001.001.003 | Inventory        | debit  | 60.00  |
      | 002.001.001 | Trade Payables   | credit | 60.00  |
    And the subledger entries for "{{deliveryReference}}" should be exactly:
      | contact                          | payable | receivable | running payable | running receivable |
      | {{supplierReference:Acme Dairy}} | 60.00   | 0.00       | 60.00           | 0.00               |
    And the account balances should be:
      | account     | name             | balance |
      | 001.001.003 | Inventory        | 60.00   |
      | 001.001     | Current Assets   | 60.00   |
      | 001         | Assets           | 60.00   |
      | 002.001.001 | Trade Payables   | 60.00   |
      | 002.001     | Accounts Payable | 60.00   |
      | 002         | Liabilities      | 60.00   |
    And no other account should have a balance

  Scenario: A rejected delivery posts nothing to the books
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 11,
           "unitId": "{{unitValue}}", "unitCost": 5.00}
        ]
      }
      """
    Then the response status should be 400
    And LedgerEntryGroup table should have no records in database
    And the account balances should be:
      | account     | name             | balance |
      | 001.001.003 | Inventory        | 0.00    |
      | 002.001.001 | Trade Payables   | 0.00    |
    And no other account should have a balance
