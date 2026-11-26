@consumes-from-kafka
Feature: Receiving a delivery against an order
  As a receiving clerk
  I want to record what a supplier delivered against an order
  So that stock, the order status and what we may pay the supplier follow what actually arrived

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

  Scenario: A partial delivery leaves the order open and the ceiling at the order total
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "notes": "First truck",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 4,
           "unitId": "{{unitValue}}", "unitCost": 5.00}
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseStatus": "PARTIALLY_DELIVERED",
        "paymentCeiling": 80.00,
        "lines": [
          {"locationProduct": {"productName": "Milk"}, "quantityDelivered": 4, "quantityPendingDelivery": 6},
          {"locationProduct": {"productName": "Bread"}, "quantityDelivered": 0, "quantityPendingDelivery": 12}
        ]
      }
      """
    And the location product named "Milk" should have "4" on hand
    And the location product named "Bread" should have "0" on hand
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"storageQuantityDelivered": 4}
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "PARTIALLY_DELIVERED"}
      """

  Scenario: Delivering the rest completes the order and the ceiling becomes what was delivered
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 6,
           "unitId": "{{unitValue}}", "unitCost": 5.00},
          {"purchaseLineId": "{{purchaseLineId:Bread}}", "quantityDelivered": 12,
           "unitId": "{{unitValue}}", "unitCost": 2.50}
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseStatus": "FULLY_DELIVERED",
        "paymentCeiling": 80.00,
        "lines": [
          {"locationProduct": {"productName": "Milk"}, "quantityDelivered": 10, "quantityPendingDelivery": 0},
          {"locationProduct": {"productName": "Bread"}, "quantityDelivered": 12, "quantityPendingDelivery": 0}
        ]
      }
      """
    And the location product named "Milk" should have "10" on hand
    And the location product named "Bread" should have "12" on hand
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"storageQuantityDelivered": 10}
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "FULLY_DELIVERED"}
      """

  Scenario: A supplier who charges more than the order makes the ceiling follow the delivered total
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 10,
           "unitId": "{{unitValue}}", "unitCost": 6.00},
          {"purchaseLineId": "{{purchaseLineId:Bread}}", "quantityDelivered": 12,
           "unitId": "{{unitValue}}", "unitCost": 2.50}
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"purchaseStatus": "FULLY_DELIVERED", "paymentCeiling": 90.00}
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "FULLY_DELIVERED"}
      """
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"unitCost": 5.00, "storageQuantityDelivered": 10}
      """

  Scenario: Delivering more than is pending is rejected and nothing is written
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Bread}}", "quantityDelivered": 3,
           "unitId": "{{unitValue}}", "unitCost": 2.50},
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 11,
           "unitId": "{{unitValue}}", "unitCost": 5.00}
        ]
      }
      """
    Then the response status should be 400
    And the response error should contain "You can deliver at most 10 Pieces of"
    And the response error should contain "but you entered 11 Pieces."
    And PurchaseDelivery table should have no records in database
    And StockEntry table should have no records in database
    And StockMovement table should have no records in database
    And LedgerEntryGroup table should have no records in database
    And PurchaseLine should exist in database with id {{purchaseLineId:Bread}} and options:
      """json
      {"storageQuantityDelivered": 0}
      """

  Scenario: A delivery with no lines is rejected
    When I POST to /secured/deliveries with payload:
      """json
      {"purchaseId": "{{purchaseId}}", "deliveredAt": "{{now}}", "lines": []}
      """
    Then the response status should be 400
    And the response error should contain "Delivery must have at least one line."
    And PurchaseDelivery table should have no records in database

  Scenario: The same line twice in one delivery is rejected
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 1,
           "unitId": "{{unitValue}}", "unitCost": 5.00},
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 2,
           "unitId": "{{unitValue}}", "unitCost": 5.00}
        ]
      }
      """
    Then the response status should be 400
    And the response error should contain "Duplicate purchase line IDs in delivery request."
    And PurchaseDelivery table should have no records in database

  Scenario: A delivery dated in the future is rejected
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{dayAhead}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 1,
           "unitId": "{{unitValue}}", "unitCost": 5.00}
        ]
      }
      """
    Then the response status should be 400
    And the response error should contain "Delivery date cannot be in the future"
    And PurchaseDelivery table should have no records in database
    And LedgerEntryGroup table should have no records in database

  Scenario: A delivery against a canceled order is rejected
    Given the following quantities are canceled on the purchase:
      | product | quantity |
      | Milk    | 10       |
      | Bread   | 12       |
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 1,
           "unitId": "{{unitValue}}", "unitCost": 5.00}
        ]
      }
      """
    Then the response status should be 400
    And PurchaseDelivery table should have no records in database
    And StockEntry table should have no records in database

  Scenario: A delivery against a draft is rejected until the order is confirmed
    Given a draft purchase from "Acme Dairy" with lines:
      | product | quantity | unit cost |
      | Milk    | 5        | 5.00      |
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityDelivered": 1,
           "unitId": "{{unitValue}}", "unitCost": 5.00}
        ]
      }
      """
    Then the response status should be 400
    And the response error should contain "Cannot record a delivery on a draft purchase. Confirm the order first."
    And PurchaseDelivery table should have no records in database
