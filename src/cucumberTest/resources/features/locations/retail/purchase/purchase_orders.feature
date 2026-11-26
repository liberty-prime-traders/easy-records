@consumes-from-kafka
Feature: Ordering from a supplier
  As a buyer
  I want to save draft purchases and place orders with a supplier
  So that what I plan to buy is recorded without touching stock or the books until it is delivered

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And a product named "Milk" exists
    And a product named "Bread" exists
    And a supplier named "Acme Dairy" exists
    And the location product named "Milk" is available
    And the location product named "Bread" is available

  Scenario: A draft purchase is saved with its lines and posts nothing
    When I POST to /secured/purchases/draft with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "notes": "Weekly restock",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Milk}}",
            "quantityOrdered": 10,
            "unitId": "{{unitValue}}",
            "unitCost": 5.00
          },
          {
            "locationProductId": "{{locationProductId:Bread}}",
            "quantityOrdered": 12,
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
        "purchaseStatus": "DRAFT",
        "paymentStatus": "UNPAID",
        "supplierName": "Acme Dairy",
        "notes": "Weekly restock",
        "orderedTotal": 80,
        "deliveredTotal": 0,
        "lines": [
          {"locationProduct": {"productName": "Milk"}, "quantityOrdered": 10, "unitCost": 5, "lineTotal": 50},
          {"locationProduct": {"productName": "Bread"}, "quantityOrdered": 12, "unitCost": 2.5, "lineTotal": 30}
        ]
      }
      """
    And I remember the response field "id" as "purchaseId"
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "DRAFT", "supplierId": "{{supplierId}}", "notes": "Weekly restock"}
      """
    And PurchaseLine table should contain exactly these records:
      """json
      [
        {"locationProductId": "{{locationProductId:Milk}}", "userProvidedQuantityOrdered": 10, "unitCost": 5},
        {"locationProductId": "{{locationProductId:Bread}}", "userProvidedQuantityOrdered": 12, "unitCost": 2.5}
      ]
      """
    And LedgerEntryGroup table should have no records in database
    And SubledgerEntry table should have no records in database
    And StockEntry table should have no records in database
    And no other account should have a balance

  Scenario: A draft line set to quantity 0 is removed and the other line stays
    Given a draft purchase from "Acme Dairy" with lines:
      | product | quantity | unit cost |
      | Milk    | 10       | 5.00      |
      | Bread   | 12       | 2.50      |
    When I PUT to /secured/purchases/draft with payload:
      """json
      {
        "id": "{{purchaseId}}",
        "linesToUpdate": [
          {
            "id": "{{purchaseLineId:Milk}}",
            "quantityOrdered": 0,
            "unitId": "{{unitValue}}",
            "unitCost": 5.00
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {
        "purchaseStatus": "DRAFT",
        "orderedTotal": 30,
        "lines": [
          {"locationProduct": {"productName": "Bread"}, "quantityOrdered": 12, "unitCost": 2.5, "lineTotal": 30}
        ]
      }
      """
    And PurchaseLine table should contain exactly these records:
      """json
      [{"locationProductId": "{{locationProductId:Bread}}", "userProvidedQuantityOrdered": 12, "unitCost": 2.5}]
      """

  Scenario: A draft becomes an order without posting to stock or the books
    Given a draft purchase from "Acme Dairy" with lines:
      | product | quantity | unit cost |
      | Milk    | 10       | 5.00      |
      | Bread   | 12       | 2.50      |
    When I PUT to /secured/purchases/order with payload:
      """json
      {"id": "{{purchaseId}}", "notes": "Confirmed by phone"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseStatus": "ORDERED",
        "paymentStatus": "UNPAID",
        "notes": "Confirmed by phone",
        "orderedTotal": 80,
        "deliveredTotal": 0,
        "paymentCeiling": 80
      }
      """
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "ORDERED", "notes": "Confirmed by phone"}
      """
    And PurchaseLine table should have exactly 2 records in database
    And LedgerEntryGroup table should have no records in database
    And SubledgerEntry table should have no records in database
    And StockEntry table should have no records in database
    And no other account should have a balance

  Scenario: An order dated years ago is accepted as given
    When I POST to /secured/purchases/order with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "dateOrdered": "2020-03-15T10:00:00Z",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Milk}}",
            "quantityOrdered": 10,
            "unitId": "{{unitValue}}",
            "unitCost": 5.00
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"purchaseStatus": "ORDERED", "dateOrdered": "2020-03-15T10:00:00Z", "orderedTotal": 50}
      """
    And I remember the response field "id" as "purchaseId"
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"purchaseStatus": "ORDERED"}
      """

  Scenario: An order with no lines is rejected and nothing is saved
    When I POST to /secured/purchases/order with payload:
      """json
      {"supplierId": "{{supplierId:Acme Dairy}}", "linesToAdd": []}
      """
    Then the response status should be 400
    And the response error message should be "An order must have at least one item."
    And Purchase table should have no records in database
    And PurchaseLine table should have no records in database

  Scenario: An order for a deactivated product is rejected and nothing is saved
    Given the location product named "Milk" is deactivated
    When I POST to /secured/purchases/order with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Milk}}",
            "quantityOrdered": 10,
            "unitId": "{{unitValue}}",
            "unitCost": 5.00
          },
          {
            "locationProductId": "{{locationProductId:Bread}}",
            "quantityOrdered": 12,
            "unitId": "{{unitValue}}",
            "unitCost": 2.50
          }
        ]
      }
      """
    Then the response status should be 400
    And the response error message should be "Inactive products are not allowed: {{locationProductReference:Milk}}/Milk"
    And Purchase table should have no records in database
    And PurchaseLine table should have no records in database

  Scenario: Notes on an order can be changed and cleared
    Given an order from "Acme Dairy" with lines:
      | product    | quantity | unit cost |
      | Milk | 10       | 5.00      |
    When I PUT to /secured/purchases/{{purchaseId}}/notes with payload:
      """json
      {"notes": "Call before delivery"}
      """
    Then the response status should be 200
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"notes": "Call before delivery"}
      """
    When I PUT to /secured/purchases/{{purchaseId}}/notes with payload:
      """json
      {"notes": null}
      """
    Then the response status should be 200
    And Purchase should exist in database with id {{purchaseId}} and options:
      """json
      {"notes": null}
      """
