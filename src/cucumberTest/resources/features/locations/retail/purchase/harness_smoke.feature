Feature: Harness smoke test (throwaway)
  Proves the phase 0 steps can run the chain supplier, product, order, delivery and read the books back.
  Delete this file once phase 1 and phase 2 features exist.

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And a product named "Whole Milk" exists
    And a supplier named "Acme Dairy" exists
    And the location product named "Whole Milk" is available

  @consumes-from-kafka
  Scenario: A delivery of 10 cartons at 5.00 puts 50.00 into inventory and owes the supplier 50.00
    When I POST to /secured/purchases/order with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Whole Milk}}",
            "quantityOrdered": 10,
            "unitId": "{{unitValue}}",
            "unitCost": 5.00
          }
        ]
      }
      """
    Then the response status should be 200
    And I remember the response field "id" as "purchaseId"
    And I remember the response field "lines[0].id" as "purchaseLineId"
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId}}", "quantityDelivered": 10, "unitId": "{{unitValue}}", "unitCost": 5.00}
        ]
      }
      """
    Then the response status should be 200
    And I remember the response field "deliveries[0].referenceNumber" as "deliveryReference"
    And the ledger group for "{{deliveryReference}}" should have source type "PURCHASE_DELIVERY"
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name             | type   | amount |
      | 001.001.003 | Inventory        | debit  | 50.00  |
      | 002.001.001 | Trade Payables   | credit | 50.00  |
    And the ledger entries for "{{deliveryReference}}" should balance
    And the subledger entries for "{{deliveryReference}}" should be exactly:
      | contact                         | payable | receivable | running payable | running receivable |
      | {{supplierReference:Acme Dairy}} | 50.00   | 0.00       | 50.00           | 0.00               |
    And the account balances should be:
      | account     | name             | balance |
      | 001.001.003 | Inventory        | 50.00   |
      | 001.001     | Current Assets   | 50.00   |
      | 001         | Assets           | 50.00   |
      | 002.001.001 | Trade Payables   | 50.00   |
      | 002.001     | Accounts Payable | 50.00   |
      | 002         | Liabilities      | 50.00   |
    And no other account should have a balance
    And every parent account should equal the sum of its children
    And the location product named "Whole Milk" should have "10" on hand
