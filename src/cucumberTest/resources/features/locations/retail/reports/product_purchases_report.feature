@consumes-from-kafka @past-fiscal-periods
Feature: Product purchases report
  As a buyer
  I want to see what each product received from suppliers and what it cost
  So that I can compare suppliers and see what a base unit cost on average

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And the system unit "dz" is selected
    And a product named "Milk" with base unit "pc" exists
    And a product named "Bread" with base unit "pc" exists
    And a product named "Soda" with base unit "dz" exists
    And a supplier named "Acme Dairy" exists
    And a supplier named "Bulk Bakers" exists
    And the location product named "Milk" is available
    And the location product named "Bread" is available
    And the location product named "Soda" is available
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit |
      | Milk    | 10       | 5.00      | pc   |
      | Bread   | 8        | 2.00      | pc   |
    And a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Milk    | 4        | 5.00      | pc   |
      | Bread   | 5        | 2.00      | pc   |
    And the location product named "Milk" should have "4" on hand
    And the location product named "Bread" should have "5" on hand
    And an order from "Bulk Bakers" with lines:
      | product | quantity | unit cost | unit |
      | Milk    | 10       | 6.00      | pc   |
    And a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Milk    | 6        | 6.00      | pc   |
    And the location product named "Milk" should have "10" on hand

  Scenario: Each product shows its delivery lines, quantity, total cost and average cost per base unit
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {"deliveredFrom": "{{dayAgo}}", "deliveredBefore": "{{dayAhead}}"}
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      [
        {
          "productName": "Bread",
          "productGroupName": "Dairy",
          "categoryName": "Grocery",
          "deliveryLineCount": 1,
          "receivedQuantityBreakdown": "5 Pieces",
          "totalCost": 10.00,
          "averageCostPerBaseUnit": "KES 2.00"
        },
        {
          "productName": "Milk",
          "productGroupName": "Dairy",
          "categoryName": "Grocery",
          "deliveryLineCount": 2,
          "receivedQuantityBreakdown": "10 Pieces",
          "totalCost": 56.00,
          "averageCostPerBaseUnit": "KES 5.60"
        }
      ]
      """

  Scenario: Filtering by supplier counts only what that supplier delivered
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {
        "deliveredFrom": "{{dayAgo}}",
        "deliveredBefore": "{{dayAhead}}",
        "supplierIds": ["{{supplierId:Bulk Bakers}}"]
      }
      """
    Then the response status should be 200
    And the response should contain 1 items
    And response contains details with exact lists in order:
      """json
      [
        {
          "productName": "Milk",
          "deliveryLineCount": 1,
          "receivedQuantityBreakdown": "6 Pieces",
          "totalCost": 36.00,
          "averageCostPerBaseUnit": "KES 6.00"
        }
      ]
      """

  Scenario: Filtering by product leaves out the others
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {
        "deliveredFrom": "{{dayAgo}}",
        "deliveredBefore": "{{dayAhead}}",
        "locationProductIds": ["{{locationProductId:Bread}}"]
      }
      """
    Then the response status should be 200
    And the response should contain 1 items
    And response contains details:
      """json
      {"productName": "Bread", "totalCost": 10.00}
      """

  Scenario: Each date range counts only the deliveries made inside it
    Given a delivery dated "{{dayAgo}}" is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Milk    | 2        | 6.00      | pc   |
    And the location product named "Milk" should have "12" on hand
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {
        "deliveredFrom": "{{twoDaysAgo}}",
        "deliveredBefore": "{{halfDayAgo}}",
        "locationProductIds": ["{{locationProductId:Milk}}"]
      }
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      [{"productName": "Milk", "deliveryLineCount": 1, "receivedQuantityBreakdown": "2 Pieces", "totalCost": 12.00}]
      """
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {
        "deliveredFrom": "{{halfDayAgo}}",
        "deliveredBefore": "{{dayAhead}}",
        "locationProductIds": ["{{locationProductId:Milk}}"]
      }
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      [{"productName": "Milk", "deliveryLineCount": 2, "receivedQuantityBreakdown": "10 Pieces", "totalCost": 56.00}]
      """
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {
        "deliveredFrom": "{{twoDaysAgo}}",
        "deliveredBefore": "{{dayAhead}}",
        "locationProductIds": ["{{locationProductId:Milk}}"]
      }
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      [{"productName": "Milk", "deliveryLineCount": 3, "receivedQuantityBreakdown": "12 Pieces", "totalCost": 68.00}]
      """
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {"deliveredFrom": "{{dayAhead}}", "deliveredBefore": "{{dayAhead}}"}
      """
    Then the response status should be 200
    And the response should be an empty list

  Scenario: A dozen based product is averaged per dozen whatever unit it was received in
    Given an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit |
      | Soda    | 2        | 60.00     | dz   |
    And a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Soda    | 12       | 6.00      | pc   |
    And the location product named "Soda" should have "1" on hand
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {
        "deliveredFrom": "{{dayAgo}}",
        "deliveredBefore": "{{dayAhead}}",
        "locationProductIds": ["{{locationProductId:Soda}}"]
      }
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      [
        {
          "productName": "Soda",
          "deliveryLineCount": 1,
          "receivedQuantityBreakdown": "1 Dozen",
          "totalCost": 72.00,
          "averageCostPerBaseUnit": "KES 72.00"
        }
      ]
      """

  Scenario: Expanding a product lists its delivery lines newest first
    When I POST to /secured/product-purchases-report/{{locationProductId:Milk}}/delivery-lines with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {"deliveredFrom": "{{dayAgo}}", "deliveredBefore": "{{dayAhead}}"}
      }
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      {
        "hasMore": false,
        "contents": [
          {
            "purchaseReferenceNumber": "{{purchaseReference}}",
            "deliveryReferenceNumber": "{{deliveryReference}}",
            "supplierName": "Bulk Bakers",
            "quantityDeliveredText": "6 Pieces",
            "baseQuantity": 6,
            "unitCost": 6.00,
            "lineTotal": 36.00,
            "status": "ACTIVE"
          },
          {
            "supplierName": "Acme Dairy",
            "quantityDeliveredText": "4 Pieces",
            "baseQuantity": 4,
            "unitCost": 5.00,
            "lineTotal": 20.00,
            "status": "ACTIVE"
          }
        ]
      }
      """

  Scenario: The expansion pages through the lines with its cursor
    When I POST to /secured/product-purchases-report/{{locationProductId:Milk}}/delivery-lines with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 1,
        "parameters": {"deliveredFrom": "{{dayAgo}}", "deliveredBefore": "{{dayAhead}}"}
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"hasMore": true, "contents": [{"supplierName": "Bulk Bakers", "lineTotal": 36.00}]}
      """
    And I remember the response field "currentCursor" as "searchCursor"
    When I POST to /secured/product-purchases-report/{{locationProductId:Milk}}/delivery-lines with payload:
      """json
      {
        "previousCursor": "{{searchCursor}}",
        "requestedSize": 1,
        "parameters": {"deliveredFrom": "{{dayAgo}}", "deliveredBefore": "{{dayAhead}}"}
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"hasMore": false, "contents": [{"supplierName": "Acme Dairy", "lineTotal": 20.00}]}
      """

  Scenario: A summary without both dates is rejected
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {"deliveredFrom": "{{dayAgo}}"}
      """
    Then the response status should be 400
    And the response error should contain "deliveredFrom and deliveredBefore are required"

  Scenario: A summary whose dates are the wrong way round is rejected
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {"deliveredFrom": "{{dayAhead}}", "deliveredBefore": "{{dayAgo}}"}
      """
    Then the response status should be 400
    And the response error should contain "deliveredFrom must not be after deliveredBefore"

  Scenario: A summary over more than a year is rejected
    When I POST to /secured/product-purchases-report/summary with payload:
      """json
      {"deliveredFrom": "2020-01-01T00:00:00Z", "deliveredBefore": "{{dayAhead}}"}
      """
    Then the response status should be 400
    And the response error should contain "must be at most 1 year apart"

  Scenario: An expansion page size outside the allowed range is rejected
    When I POST to /secured/product-purchases-report/{{locationProductId:Milk}}/delivery-lines with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 0,
        "parameters": {"deliveredFrom": "{{dayAgo}}", "deliveredBefore": "{{dayAhead}}"}
      }
      """
    Then the response status should be 400
    And the response error should contain "requestedSize must be between 1 and 500"
