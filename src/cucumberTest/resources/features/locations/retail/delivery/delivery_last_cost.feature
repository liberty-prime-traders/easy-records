@consumes-from-kafka
Feature: Last purchase price of a product
  As a buyer
  I want each product to remember what a base unit cost on its latest delivery
  So that I can compare the next quote with what I last paid

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
      | Eggs    | 3        | 60.00     | dz   |
      | Soda    | 2        | 60.00     | dz   |

  Scenario: A product with no delivery has no last purchase price
    Then LocationProduct should exist in database with id {{locationProductId:Eggs}} and options:
      """json
      {"lastPurchasePrice": null}
      """

  Scenario: A dozen delivered at the dozen price gives the price of one piece
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 1        | 60.00     | dz   |
    Then the location product named "Eggs" should have "12" on hand
    And LocationProduct should exist in database with id {{locationProductId:Eggs}} and options:
      """json
      {"lastPurchasePrice": 5.0000}
      """

  Scenario: The latest delivery replaces the earlier price
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 1        | 60.00     | dz   |
    And the location product named "Eggs" should have "12" on hand
    When a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 12       | 6.00      | pc   |
    Then the location product named "Eggs" should have "24" on hand
    And LocationProduct should exist in database with id {{locationProductId:Eggs}} and options:
      """json
      {"lastPurchasePrice": 6.0000}
      """

  Scenario: A dozen that does not divide evenly is kept to four decimals
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Eggs    | 1        | 10.00     | dz   |
    Then the location product named "Eggs" should have "12" on hand
    And LocationProduct should exist in database with id {{locationProductId:Eggs}} and options:
      """json
      {"lastPurchasePrice": 0.8333}
      """

  Scenario: A product whose base unit is a dozen gets the price of a whole dozen
    Given a delivery is recorded on the purchase with lines:
      | product | quantity | unit cost | unit |
      | Soda    | 12       | 6.00      | pc   |
    Then the location product named "Soda" should have "1" on hand
    And LocationProduct should exist in database with id {{locationProductId:Soda}} and options:
      """json
      {"lastPurchasePrice": 72.0000}
      """

  Scenario: A rejected delivery leaves the price untouched
    When I POST to /secured/deliveries with payload:
      """json
      {
        "purchaseId": "{{purchaseId}}",
        "deliveredAt": "{{now}}",
        "lines": [
          {
            "purchaseLineId": "{{purchaseLineId:Eggs}}",
            "quantityDelivered": 4,
            "unitId": "{{unitValue:dz}}",
            "unitCost": 60.00
          }
        ]
      }
      """
    Then the response status should be 400
    And the response error should contain "You can deliver at most"
    And LocationProduct should exist in database with id {{locationProductId:Eggs}} and options:
      """json
      {"lastPurchasePrice": null}
      """
