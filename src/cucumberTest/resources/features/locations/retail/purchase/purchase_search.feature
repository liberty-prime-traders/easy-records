@consumes-from-kafka
Feature: Finding purchases
  As a buyer
  I want to search purchases by status, supplier and reference
  So that I can see what is open with each supplier

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
    And a draft purchase from "Acme Dairy" with lines:
      | product | quantity | unit cost |
      | Milk    | 2        | 5.00      |
    And an order from "Bulk Bakers" with lines:
      | product | quantity | unit cost |
      | Bread   | 40       | 3.00      |
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost |
      | Milk    | 4        | 5.00      |
    And the following quantities are canceled on the purchase:
      | product | quantity |
      | Milk    | 4        |

  Scenario Outline: Purchases are found by status, supplier, reference, amount and payment status
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {"recordedFrom": "{{dayAgo}}", "recordedBefore": "{{dayAhead}}", <filter>}
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": <purchases>}
      """

    Examples:
      | filter                                                   | purchases                                         |
      | "purchaseStatuses": ["DRAFT"]                            | [{"referenceNumber": "{{purchaseReference->1}}"}] |
      | "supplierIds": ["{{supplierId:Bulk Bakers}}"]            | [{"referenceNumber": "{{purchaseReference->2}}"}] |
      | "purchaseReferenceNumbers": ["{{purchaseReference->0}}"] | [{"referenceNumber": "{{purchaseReference->0}}"}] |
      | "minAmount": 100                                         | [{"referenceNumber": "{{purchaseReference->2}}"}] |
      | "minAmount": 50, "maxAmount": 100                        | [{"referenceNumber": "{{purchaseReference->0}}"}] |
      | "maxAmount": 0                                           | [{"referenceNumber": "{{purchaseReference->3}}"}] |
      | "paymentStatuses": ["FULLY_SETTLED"]                     | []                                                |

  Scenario: Filters are combined, so an ordered purchase from one supplier leaves out its draft and canceled ones
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {
          "recordedFrom": "{{dayAgo}}",
          "recordedBefore": "{{dayAhead}}",
          "purchaseStatuses": ["ORDERED"],
          "supplierIds": ["{{supplierId:Acme Dairy}}"]
        }
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": [{"referenceNumber": "{{purchaseReference->0}}", "supplierName": "Acme Dairy", "orderedTotal": 80}]}
      """

  Scenario: A purchase date range leaves out the draft because it has no order date
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {
          "recordedFrom": "{{dayAgo}}",
          "recordedBefore": "{{dayAhead}}",
          "purchaseDateFrom": "{{dayAgo}}",
          "purchaseDateBefore": "{{dayAhead}}"
        }
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {
        "contents": [
          {"referenceNumber": "{{purchaseReference->0}}"},
          {"referenceNumber": "{{purchaseReference->2}}"},
          {"referenceNumber": "{{purchaseReference->3}}"}
        ]
      }
      """

  Scenario: Purchases recorded outside the window are not found
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {"recordedFrom": "2020-01-01T00:00:00Z", "recordedBefore": "2020-06-01T00:00:00Z"}
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": []}
      """

  Scenario: Purchases are listed newest first and paged by cursor
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 2,
        "parameters": {"recordedFrom": "{{dayAgo}}", "recordedBefore": "{{dayAhead}}"}
      }
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      {
        "hasMore": true,
        "contents": [
          {"referenceNumber": "{{purchaseReference->3}}", "purchaseStatus": "CANCELED"},
          {"referenceNumber": "{{purchaseReference->2}}", "purchaseStatus": "ORDERED"}
        ]
      }
      """
    And I remember the response field "currentCursor" as "searchCursor"
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "{{searchCursor}}",
        "requestedSize": 2,
        "parameters": {"recordedFrom": "{{dayAgo}}", "recordedBefore": "{{dayAhead}}"}
      }
      """
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      {
        "hasMore": false,
        "contents": [
          {"referenceNumber": "{{purchaseReference->1}}", "purchaseStatus": "DRAFT"},
          {"referenceNumber": "{{purchaseReference->0}}", "purchaseStatus": "ORDERED"}
        ]
      }
      """

  Scenario: A search without a recorded window is rejected
    When I POST to /secured/purchases/search with payload:
      """json
      {"previousCursor": "", "requestedSize": 10, "parameters": {}}
      """
    Then the response status should be 400
    And the response error message should be "recordedFrom and recordedBefore are required"

  Scenario: The summary of all purchases counts drafts and canceled ones at what is still ordered
    When I POST to /secured/purchases/summary with payload:
      """json
      {"recordedFrom": "{{dayAgo}}", "recordedBefore": "{{dayAhead}}"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseCount": 4,
        "totalOrdered": 210,
        "totalDelivered": 0,
        "totalPaid": 0,
        "totalOutstanding": 0,
        "byPaymentStatus": [
          {"paymentStatus": "UNPAID", "purchaseCount": 4, "totalOrdered": 210, "totalDelivered": 0, "totalPaid": 0}
        ]
      }
      """

  Scenario: The summary of one supplier's purchases adds a line for that supplier
    When I POST to /secured/purchases/summary with payload:
      """json
      {"recordedFrom": "{{dayAgo}}", "recordedBefore": "{{dayAhead}}", "supplierIds": ["{{supplierId:Acme Dairy}}"]}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "purchaseCount": 3,
        "totalOrdered": 90,
        "bySupplier": [
          {"supplierName": "Acme Dairy", "purchaseCount": 3, "totalOrdered": 90, "totalDelivered": 0, "totalPaid": 0}
        ]
      }
      """

  Scenario: The most recent purchases are listed newest first
    When I GET to /secured/purchases?n=2
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      [
        {"referenceNumber": "{{purchaseReference->3}}", "purchaseStatus": "CANCELED"},
        {"referenceNumber": "{{purchaseReference->2}}", "purchaseStatus": "ORDERED"}
      ]
      """

  Scenario: The amount filter prices a partly canceled line at what is still ordered
    Given an order from "Acme Dairy" with lines:
      | product | quantity | unit cost |
      | Milk    | 10       | 5.00      |
    And the following quantities are canceled on the purchase:
      | product | quantity |
      | Milk    | 3        |
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {
          "recordedFrom": "{{dayAgo}}",
          "recordedBefore": "{{dayAhead}}",
          "minAmount": 35.00,
          "maxAmount": 35.00
        }
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": [{"referenceNumber": "{{purchaseReference->4}}"}]}
      """
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {
          "recordedFrom": "{{dayAgo}}",
          "recordedBefore": "{{dayAhead}}",
          "minAmount": 35.01,
          "maxAmount": 50.00
        }
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": []}
      """

  Scenario: The amount range is inclusive at a total built from a fractional cost, a unit factor and a cancel
    Given a unit group named "Beverages" with base unit "Bottle" coded "btl" exists
    And a unit named "Trio" coded "trio" equal to 3 of unit "btl" exists
    And a product named "Cola" with base unit "btl" exists
    And the location product named "Cola" is available
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit |
      | Cola    | 7        | 3.35      | trio |
    And I PUT to /secured/purchases/{{purchaseId}}/line-cancel-quantities with payload:
      """json
      [{"purchaseLineId": "{{purchaseLineId:Cola}}", "quantityCanceled": 5, "unitId": "{{unitValue:btl}}"}]
      """
    And PurchaseLine should exist in database with id {{purchaseLineId:Cola}} and options:
      """json
      {"storageQuantityCanceled": 5, "storageUnitsPerUserProvidedUnit": 3}
      """
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {
          "recordedFrom": "{{dayAgo}}",
          "recordedBefore": "{{dayAhead}}",
          "minAmount": 17.87,
          "maxAmount": 17.87
        }
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": [{"referenceNumber": "{{purchaseReference->4}}"}]}
      """
    When I POST to /secured/purchases/search with payload:
      """json
      {
        "previousCursor": "",
        "requestedSize": 10,
        "parameters": {
          "recordedFrom": "{{dayAgo}}",
          "recordedBefore": "{{dayAhead}}",
          "minAmount": 17.88,
          "maxAmount": 17.89
        }
      }
      """
    Then the response status should be 200
    And response contains details with exact lists:
      """json
      {"contents": []}
      """
