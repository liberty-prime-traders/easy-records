@consumes-from-kafka @past-fiscal-periods
Feature: Completing orders by delivery
  As a receiving clerk
  I want orders to complete however they are delivered
  So that the order status, what we may pay and what we owe each supplier stay right

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And a product named "Milk" exists
    And a product named "Bread" exists
    And a supplier named "Acme Dairy" exists
    And a supplier named "Bulk Bakers" exists
    And the location product named "Milk" is available
    And the location product named "Bread" is available
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost |
      | Milk    | 10       | 5.00      |
      | Bread   | 12       | 2.50      |

  Scenario: Delivering everything in one request completes the order
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "notes": "Whole order on one truck",
        "lines": [
          {
            "purchaseLineId": "{{purchaseLineId:Milk}}",
            "quantityDelivered": 10,
            "unitId": "{{unitValue}}",
            "unitCost": 5.00
          },
          {
            "purchaseLineId": "{{purchaseLineId:Bread}}",
            "quantityDelivered": 12,
            "unitId": "{{unitValue}}",
            "unitCost": 2.50
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseStatus": "FULLY_DELIVERED",
        "paymentCeiling": 80.00,
        "deliveredTotal": 80.00,
        "lines": [
          {"locationProduct": {"productName": "Milk"}, "quantityDelivered": 10, "quantityPendingDelivery": 0},
          {"locationProduct": {"productName": "Bread"}, "quantityDelivered": 12, "quantityPendingDelivery": 0}
        ]
      }
      """
    And the location product named "Milk" should have "10" on hand
    And the location product named "Bread" should have "12" on hand
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "FULLY_DELIVERED"}
      """
    And PurchaseDelivery table should contain exactly these records:
      """json
      [{"notes": "Whole order on one truck", "status": "RECEIVED"}]
      """

  Scenario: What was canceled is not owed, so the rest completes the order and the ceiling is what arrived
    Given the following quantities are canceled on the purchase:
      | product | quantity |
      | Milk    | 4        |
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 6        | 5.00      |
      | Bread   | 12       | 2.50      |
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name           | type   | amount |
      | 001.001.003 | Inventory      | debit  | 60.00  |
      | 002.001.001 | Trade Payables | credit | 60.00  |
    When I GET to /secured/purchases
    Then response contains item with details:
      """json
      {
        "referenceNumber": "{{purchaseReference}}",
        "purchaseStatus": "FULLY_DELIVERED",
        "paymentCeiling": 60.00,
        "lines": [
          {
            "locationProduct": {"productName": "Milk"},
            "quantityNetOrdered": 6,
            "quantityDelivered": 6,
            "quantityPendingDelivery": 0
          }
        ]
      }
      """

  Scenario: A delivery is posted in the fiscal period of its own date, not of the day it was recorded
    Given the past fiscal period is named "Yesterday Period"
    And the open fiscal period is named "Today Period"
    And a delivery dated "{{dayAgo}}" is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    Then the ledger group for "{{deliveryReference}}" should have source type "PURCHASE_DELIVERY"
    And the ledger group for "{{deliveryReference}}" should be posted in the fiscal period named "Yesterday Period"
    And the ledger entries for "{{deliveryReference}}" should be exactly:
      | account     | name           | type   | amount |
      | 001.001.003 | Inventory      | debit  | 20.00  |
      | 002.001.001 | Trade Payables | credit | 20.00  |
    And the location product named "Milk" should have "4" on hand
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 6        | 5.00      |
    Then the ledger group for "{{deliveryReference}}" should be posted in the fiscal period named "Today Period"
    And PurchaseDelivery table should have exactly 2 records in database

  Scenario: The supplier's payable balance grows with each delivery
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    And the subledger entries for "{{deliveryReference}}" should be exactly:
      | contact                          | payable | receivable | running payable | running receivable |
      | {{supplierReference:Acme Dairy}} | 20.00   | 0.00       | 20.00           | 0.00               |
    When I GET to /secured/contacts
    Then response contains item with details:
      """json
      {"referenceNumber": "{{supplierReference:Acme Dairy}}", "payableBalance": 20, "receivableBalance": 0}
      """
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 6        | 5.00      |
      | Bread   | 12       | 2.50      |
    And the subledger entries for "{{deliveryReference}}" should be exactly:
      | contact                          | payable | receivable | running payable | running receivable |
      | {{supplierReference:Acme Dairy}} | 60.00   | 0.00       | 80.00           | 0.00               |
    When I GET to /secured/contacts
    Then response contains item with details:
      """json
      {"referenceNumber": "{{supplierReference:Acme Dairy}}", "payableBalance": 80, "receivableBalance": 0}
      """

  Scenario: Two suppliers each carry their own payable and the books carry the sum
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    And an order from "Bulk Bakers" with lines:
      | product | quantity | unit cost |
      | Bread   | 12       | 2.50      |
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Bread   | 12       | 2.50      |
    Then the subledger entries for "{{deliveryReference}}" should be exactly:
      | contact                           | payable | receivable | running payable | running receivable |
      | {{supplierReference:Bulk Bakers}} | 30.00   | 0.00       | 30.00           | 0.00               |
    And the account balances should be:
      | account     | name           | balance |
      | 001.001.003 | Inventory      | 50.00   |
      | 002.001.001 | Trade Payables | 50.00   |
    When I GET to /secured/contacts
    Then response contains item with details:
      """json
      {"referenceNumber": "{{supplierReference:Acme Dairy}}", "payableBalance": 20}
      """
    And response contains item with details:
      """json
      {"referenceNumber": "{{supplierReference:Bulk Bakers}}", "payableBalance": 30}
      """
