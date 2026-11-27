@consumes-from-kafka
Feature: Canceling what is still undelivered
  As a buyer
  I want to cancel only the part of a line that has not arrived
  So that the order never says less than what the supplier already delivered

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

  Scenario: What can be canceled is the ordered quantity less what was delivered
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 7}]
      """
    Then the response status should be 400
    And the response error should contain "Cannot cancel 7 Pieces of line {{purchaseLineReference:Milk}}."
    And the response error should contain "Ordered: 10 Pieces, Delivered: 4 Pieces, Max cancelable: 6 Pieces"
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"storageQuantityCanceled": 0, "storageQuantityDelivered": 4}
      """

  Scenario: Canceling exactly the undelivered part is accepted and leaves nothing pending on that line
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 6}]
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseStatus": "PARTIALLY_DELIVERED",
        "paymentCeiling": 50.00,
        "lines": [
          {
            "locationProduct": {"productName": "Milk"}, "quantityDelivered": 4, "quantityCanceled": 6,
            "quantityNetOrdered": 4, "quantityPendingDelivery": 0
          },
          {
            "locationProduct": {"productName": "Bread"}, "quantityDelivered": 0, "quantityCanceled": 0,
            "quantityPendingDelivery": 12
          }
        ]
      }
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "PARTIALLY_DELIVERED"}
      """

  Scenario: The cancelable quantity shrinks as more deliveries arrive
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    And a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 3        | 5.00      |
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 4}]
      """
    Then the response status should be 400
    And the response error should contain "Ordered: 10 Pieces, Delivered: 7 Pieces, Max cancelable: 3 Pieces"
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 3}]
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "paymentCeiling": 65.00,
        "lines": [
          {
            "locationProduct": {"productName": "Milk"},
            "quantityDelivered": 7,
            "quantityCanceled": 3,
            "quantityPendingDelivery": 0
          }
        ]
      }
      """
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"storageQuantityCanceled": 3, "storageQuantityDelivered": 7}
      """

  Scenario: Canceling the undelivered rest of every line closes the order as delivered
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [
        {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 6},
        {"purchaseLineId": "{{purchaseLineId:Bread}}", "quantityCanceled": 12}
      ]
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"purchaseStatus": "FULLY_DELIVERED", "paymentCeiling": 20.00}
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "FULLY_DELIVERED"}
      """

  Scenario: An order that is fully delivered cannot have quantities canceled
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost |
      | Milk    | 10       | 5.00      |
      | Bread   | 12       | 2.50      |
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 1}]
      """
    Then the response status should be 400
    And the response error should contain "Cannot cancel lines on a purchase with status FULLY_DELIVERED"
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"storageQuantityCanceled": 0, "storageQuantityDelivered": 10}
      """
