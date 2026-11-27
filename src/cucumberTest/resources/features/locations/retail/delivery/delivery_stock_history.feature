@consumes-from-kafka
Feature: Stock history after deliveries
  As a stock controller
  I want the movement history of a product to read the way each delivery was entered
  So that I can see what arrived, in which unit, and what was on hand afterwards

  Background:
    Given a category named "Grocery" exists
    And a product group named "Dairy" exists
    And the system unit "pc" is selected
    And the system unit "dz" is selected
    And a product named "Eggs" with base unit "pc" exists
    And a product named "Soda" with base unit "dz" exists
    And a supplier named "Acme Dairy" exists
    And the location product named "Eggs" is available
    And the location product named "Soda" is available
    And an order from "Acme Dairy" with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 2        | 60.00     | dz   |
      | Soda    | 2        | 60.00     | dz   |

  Scenario: A product with no delivery has an empty history
    When I GET to /secured/location-products/history with query parameters:
      | locationProductId | {{locationProductId:Eggs}} |
    Then the response status should be 200
    And the response should be an empty list

  Scenario: Each delivery adds a movement, newest first, in the unit it was entered
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 1        | 60.00     | dz   |
    And the location product named "Eggs" should have "12" on hand
    And a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 6        | 5.00      | pc   |
    And the location product named "Eggs" should have "18" on hand
    When I GET to /secured/location-products/history with query parameters:
      | locationProductId | {{locationProductId:Eggs}} |
    Then the response status should be 200
    And the response should contain 2 items
    And response contains details with exact lists in order:
      """json
      [
        {
          "movementType": "PURCHASE_RECEIVED",
          "documentReference": "{{deliveryReference}}",
          "quantityMoved": "6 Pieces",
          "quantityEntered": "entered 6 Pieces",
          "newQuantity": "18 Pieces",
          "context": "Purchase {{purchaseReference}}"
        },
        {
          "movementType": "PURCHASE_RECEIVED",
          "quantityMoved": "12 Pieces",
          "quantityEntered": "entered 1 Dozen",
          "newQuantity": "12 Pieces",
          "context": "Purchase {{purchaseReference}}"
        }
      ]
      """

  Scenario: A dozen based product shows what arrived as dozens and pieces
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Soda    | 18       | 5.00      | pc   |
    And the location product named "Soda" should have "1.5" on hand
    When I GET to /secured/location-products/history with query parameters:
      | locationProductId | {{locationProductId:Soda}} |
    Then the response status should be 200
    And response contains details with exact lists in order:
      """json
      [
        {
          "movementType": "PURCHASE_RECEIVED",
          "quantityMoved": "1 Dozen 1 Half Dozen",
          "quantityEntered": "entered 18 Pieces",
          "newQuantity": "1 Dozen 1 Half Dozen"
        }
      ]
      """

  Scenario: A rejected delivery leaves no movement in the history
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {
            "purchaseLineId": "{{purchaseLineId:Eggs}}",
            "quantityDelivered": 3,
            "unitId": "{{unitValue:dz}}",
            "unitCost": 60.00
          }
        ]
      }
      """
    Then the response status should be 400
    And the response error should contain "You can deliver at most"
    When I GET to /secured/location-products/history with query parameters:
      | locationProductId | {{locationProductId:Eggs}} |
    Then the response should be an empty list
