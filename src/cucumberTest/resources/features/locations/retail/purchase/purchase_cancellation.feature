@consumes-from-kafka
Feature: Canceling ordered quantities
  As a buyer
  I want to cancel quantities I no longer expect from a supplier
  So that the order shows only what is still open and a fully canceled order is closed

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

  Scenario: Canceling part of a line leaves the rest of the order open
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 4}]
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseStatus": "ORDERED",
        "lines": [
          {
            "locationProduct": {"productName": "Milk"},
            "quantityOrdered": 10,
            "quantityCanceled": 4,
            "quantityNetOrdered": 6,
            "quantityPendingDelivery": 6
          },
          {
            "locationProduct": {"productName": "Bread"},
            "quantityOrdered": 12,
            "quantityCanceled": 0,
            "quantityNetOrdered": 12,
            "quantityPendingDelivery": 12
          }
        ]
      }
      """
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"userProvidedQuantityOrdered": 10, "storageQuantityCanceled": 4}
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "ORDERED"}
      """

  Scenario: A second cancellation replaces the first instead of adding to it
    Given the following quantities are canceled on the purchase:
      | product | quantity |
      | Milk    | 4        |
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 2}]
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"lines": [{"locationProduct": {"productName": "Milk"}, "quantityCanceled": 2, "quantityNetOrdered": 8}]}
      """
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"storageQuantityCanceled": 2}
      """

  Scenario: Canceling every line closes the order as canceled
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [
        {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 10},
        {"purchaseLineId": "{{purchaseLineId:Bread}}", "quantityCanceled": 12}
      ]
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseStatus": "CANCELED",
        "lines": [
          {"locationProduct": {"productName": "Milk"}, "quantityNetOrdered": 0, "quantityPendingDelivery": 0},
          {"locationProduct": {"productName": "Bread"}, "quantityNetOrdered": 0, "quantityPendingDelivery": 0}
        ]
      }
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "CANCELED"}
      """
    And LedgerEntryGroup table should have no records in database
    And StockEntry table should have no records in database

  Scenario: A cancellation larger than the ordered quantity is rejected and no line is changed
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [
        {"purchaseLineId": "{{purchaseLineId:Bread}}", "quantityCanceled": 3},
        {"purchaseLineId": "{{purchaseLineId:Milk}}", "quantityCanceled": 11}
      ]
      """
    Then the response status should be 400
    And the response error should contain "Cannot cancel 11 Pieces of line {{purchaseLineReference:Milk}}."
    And the response error should contain "Ordered: 10 Pieces, Delivered: 0 Pieces, Max cancelable: 10 Pieces"
    And PurchaseLine should exist in database with id {{purchaseLineId:Bread}} and options:
      """json
      {"storageQuantityCanceled": 0}
      """
    And PurchaseLine should exist in database with id {{purchaseLineId:Milk}} and options:
      """json
      {"storageQuantityCanceled": 0}
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "ORDERED"}
      """
