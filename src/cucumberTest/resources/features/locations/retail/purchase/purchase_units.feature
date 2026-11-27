@consumes-from-kafka
Feature: Ordering in different units
  As a buyer
  I want to order in dozens, half dozens or pieces whatever the product's own unit is
  So that what I type, what is stored and what is shown always agree

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And the system unit "hdz" is selected
    And the system unit "dz" is selected
    And a product named "Eggs" with base unit "pc" exists
    And a product named "Soda" with base unit "dz" exists
    And a supplier named "Acme Dairy" exists
    And the location product named "Eggs" is available
    And the location product named "Soda" is available

  Scenario: Two dozen of a piece-based product are stored as 24 pieces
    When I POST to /secured/purchases/order with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Eggs}}",
            "quantityOrdered": 2,
            "unitId": "{{unitValue:dz}}",
            "unitCost": 60.00
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "orderedTotal": 120,
        "lines": [
          {
            "locationProduct": {"productName": "Eggs"},
            "quantityOrdered": 2,
            "unitCost": 60,
            "lineTotal": 120,
            "conversionFactor": 12,
            "quantityNetOrdered": 2,
            "storageNetOrderedQty": 24,
            "quantityNetOrderedBreakdown": "24 Pieces"
          }
        ]
      }
      """
    And PurchaseLine table should contain exactly these records:
      """json
      [
        {
          "locationProductId": "{{locationProductId:Eggs}}",
          "userProvidedQuantityOrdered": 2,
          "unitId": "{{unitValue:dz}}",
          "unitCost": 60,
          "storageUnitsPerUserProvidedUnit": 12
        }
      ]
      """

  Scenario: Three half dozen of a dozen-based product are stored as 18 pieces and shown as a dozen and a half dozen
    When I POST to /secured/purchases/order with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Soda}}",
            "quantityOrdered": 3,
            "unitId": "{{unitValue:hdz}}",
            "unitCost": 30.00
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "orderedTotal": 90,
        "lines": [
          {
            "locationProduct": {"productName": "Soda"},
            "quantityOrdered": 3,
            "lineTotal": 90,
            "conversionFactor": 0.5,
            "quantityNetOrdered": 3,
            "storageNetOrderedQty": 18,
            "quantityNetOrderedBreakdown": "1 Dozen 1 Half Dozen"
          }
        ]
      }
      """
    And PurchaseLine table should contain exactly these records:
      """json
      [
        {
          "locationProductId": "{{locationProductId:Soda}}",
          "userProvidedQuantityOrdered": 3,
          "unitId": "{{unitValue:hdz}}",
          "unitCost": 30,
          "storageUnitsPerUserProvidedUnit": 6
        }
      ]
      """

  Scenario: Twenty-one pieces of a dozen-based product are shown across three levels
    When I POST to /secured/purchases/order with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Soda}}",
            "quantityOrdered": 21,
            "unitId": "{{unitValue:pc}}",
            "unitCost": 5.00
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "orderedTotal": 105,
        "lines": [
          {
            "quantityOrdered": 21,
            "lineTotal": 105,
            "storageNetOrderedQty": 21,
            "quantityNetOrderedBreakdown": "1 Dozen 1 Half Dozen 3 Pieces"
          }
        ]
      }
      """
    And PurchaseLine table should contain exactly these records:
      """json
      [
        {
          "userProvidedQuantityOrdered": 21,
          "unitId": "{{unitValue:pc}}",
          "unitCost": 5,
          "storageUnitsPerUserProvidedUnit": 1
        }
      ]
      """

  Scenario: Eleven crates of a user-defined chain are stored as 264 bottles
    Given a unit group named "Beverages" with base unit "Bottle" coded "btl" exists
    And a unit named "Six-pack" coded "sixpk" equal to 6 of unit "btl" exists
    And a unit named "Crate" coded "crate" equal to 4 of unit "sixpk" exists
    And a unit named "Pallet" coded "pallet" equal to 10 of unit "crate" exists
    And a product named "Cola" with base unit "pallet" exists
    And the location product named "Cola" is available
    When I POST to /secured/purchases/order with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Cola}}",
            "quantityOrdered": 11,
            "unitId": "{{unitValue:crate}}",
            "unitCost": 48.00
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "orderedTotal": 528,
        "lines": [
          {
            "quantityOrdered": 11,
            "lineTotal": 528,
            "conversionFactor": 0.1,
            "storageNetOrderedQty": 264,
            "quantityNetOrderedBreakdown": "1 Pallet 1 Crate"
          }
        ]
      }
      """
    And PurchaseLine table should contain exactly these records:
      """json
      [
        {
          "userProvidedQuantityOrdered": 11,
          "unitId": "{{unitValue:crate}}",
          "unitCost": 48,
          "storageUnitsPerUserProvidedUnit": 24
        }
      ]
      """

  Scenario: Changing a draft line from half dozens to dozens changes the stored conversion with it
    Given a draft purchase from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit |
      | Soda    | 3        | 30.00     | hdz  |
    When I PUT to /secured/purchases/draft with payload:
      """json
      {
        "id": "{{purchaseId}}",
        "linesToUpdate": [
          {
            "id": "{{purchaseLineId:Soda}}",
            "quantityOrdered": 2,
            "unitId": "{{unitValue:dz}}",
            "unitCost": 60.00
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "orderedTotal": 120,
        "lines": [
          {
            "quantityOrdered": 2,
            "lineTotal": 120,
            "conversionFactor": 1,
            "storageNetOrderedQty": 24,
            "quantityNetOrderedBreakdown": "2 Dozens"
          }
        ]
      }
      """
    And PurchaseLine table should contain exactly these records:
      """json
      [
        {
          "userProvidedQuantityOrdered": 2,
          "unitId": "{{unitValue:dz}}",
          "unitCost": 60,
          "storageUnitsPerUserProvidedUnit": 12
        }
      ]
      """

  Scenario: Canceling pieces on a line ordered in dozens is stored in pieces and shown in dozens
    Given an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 2        | 60.00     | dz   |
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Eggs}}", "quantityCanceled": 6, "unitId": "{{unitValue:pc}}"}]
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "orderedTotal": 90,
        "lines": [
          {
            "quantityOrdered": 2,
            "quantityCanceled": 0.5,
            "quantityNetOrdered": 1.5,
            "lineTotal": 90,
            "storageQuantityCanceled": 6,
            "storageNetOrderedQty": 18,
            "quantityCanceledBreakdown": "6 Pieces",
            "quantityNetOrderedBreakdown": "18 Pieces"
          }
        ]
      }
      """
    And PurchaseLine should exist in database with id {{purchaseLineId:Eggs}} and options:
      """json
      {"userProvidedQuantityOrdered": 2, "storageUnitsPerUserProvidedUnit": 12, "storageQuantityCanceled": 6}
      """

  Scenario: A cancellation in dozens larger than the order is rejected in the product's own units
    Given an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit |
      | Soda    | 3        | 30.00     | hdz  |
    When I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Soda}}", "quantityCanceled": 2, "unitId": "{{unitValue:dz}}"}]
      """
    Then the response status should be 400
    And the response error should contain "Cannot cancel 2 Dozens of line {{purchaseLineReference:Soda}}."
    And the response error should contain "Ordered: 1 Dozen 1 Half Dozen, Delivered: 0 Dozens,"
    And the response error should contain "Max cancelable: 1 Dozen 1 Half Dozen"
    And PurchaseLine should exist in database with id {{purchaseLineId:Soda}} and options:
      """json
      {"storageQuantityCanceled": 0}
      """

  Scenario: A unit edited between two orders gives each order line the size it was ordered with
    Given a unit group named "Beverages" with base unit "Bottle" coded "btl" exists
    And a unit named "Crate" coded "crate" equal to 12 of unit "btl" exists
    And a product named "Cola" with base unit "crate" exists
    And the location product named "Cola" is available
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit  |
      | Cola    | 2        | 120.00    | crate |
    And I PUT to /secured/unitvalues with payload:
      """json
      {
        "id": "{{unitValue:crate}}",
        "name": "Crate",
        "code": "crate",
        "baseUnit": "{{unitValue:btl}}",
        "unitsOfBasePerUnit": 10
      }
      """
    When I POST to /secured/purchases/order with payload:
      """json
      {
        "supplierId": "{{supplierId:Acme Dairy}}",
        "linesToAdd": [
          {
            "locationProductId": "{{locationProductId:Cola}}",
            "quantityOrdered": 2,
            "unitId": "{{unitValue:crate}}",
            "unitCost": 100.00
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "orderedTotal": 200,
        "lines": [
          {
            "quantityOrdered": 2,
            "lineTotal": 200,
            "storageNetOrderedQty": 20,
            "quantityNetOrderedBreakdown": "2 Crates"
          }
        ]
      }
      """
    And PurchaseLine table should contain exactly these records:
      """json
      [
        {
          "userProvidedQuantityOrdered": 2,
          "unitCost": 120,
          "storageUnitsPerUserProvidedUnit": 12
        },
        {
          "userProvidedQuantityOrdered": 2,
          "unitCost": 100,
          "storageUnitsPerUserProvidedUnit": 10
        }
      ]
      """
