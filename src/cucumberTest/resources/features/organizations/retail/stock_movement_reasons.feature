Feature: Stock movement reasons
  As an authenticated organization user
  I want to manage the reasons a stock movement can carry
  So that every reason is unique and applies to the movement types it lists

  Scenario: Every organization starts with the system reasons
    When I GET to /secured/stock-movement-reasons
    Then the response status should be 200
    And response contains details:
      """json
      [
        {"code": "DMG", "name": "Damaged", "systemDefined": true},
        {"code": "QI", "name": "Quality Issue", "systemDefined": true},
        {"code": "THF", "name": "Theft", "systemDefined": true},
        {"code": "CD", "name": "Count Discrepancy", "systemDefined": true},
        {"code": "EXP", "name": "Expired", "systemDefined": true}
      ]
      """

  Scenario: Creating a reason stores it as an organization reason
    When I POST to /secured/stock-movement-reasons with payload:
      """json
      {"name": "Packaging", "description": "Stock used as packaging", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE"]}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Packaging", "systemDefined": false}
      """
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Packaging", "systemDefined": false}
      """
    And Stock_Movement_Reason table should have exactly 6 records in database

  Scenario: A reason can apply to several movement types
    When I POST to /secured/stock-movement-reasons with payload:
      """json
      {"name": "Write Off", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE", "CUSTOMER_RETURN"]}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Write Off", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE", "CUSTOMER_RETURN"]}
      """
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Write Off", "movementTypes": ["CUSTOMER_RETURN"]}
      """
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Write Off", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE", "CUSTOMER_RETURN"]}
      """

  Scenario Outline: A reason needs a name and at least one known movement type
    When I POST to /secured/stock-movement-reasons with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    And Stock_Movement_Reason table should have exactly 5 records in database

    Examples:
      | payload                                                | message                                                    |
      | {"name": "", "movementTypes": ["SALE"]}                | Name cannot be null or empty                               |
      | {"name": "   ", "movementTypes": ["SALE"]}             | Name cannot be null or empty                               |
      | {"name": "Packaging", "movementTypes": []}             | A movement reason must apply to at least one movement type |
      | {"name": "Packaging"}                                  | The request body could not be read                         |
      | {"movementTypes": ["SALE"]}                            | The request body could not be read                         |
      | {"name": "Packaging", "movementTypes": ["NOT_A_TYPE"]} | The request body could not be read                         |

  Scenario Outline: A reason name is unique regardless of case and spaces
    Given a stock movement reason named "Packaging" exists
    When I POST to /secured/stock-movement-reasons with payload:
      """json
      {"name": "<name>", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE"]}
      """
    Then the response status should be 400
    And the response error message should be "A movement reason with the name '<name>' already exists"
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Packaging"}
      """
    And Stock_Movement_Reason table should have exactly 6 records in database

    Examples:
      | name       |
      | packaging  |
      | Pack aging |

  Scenario: A reason cannot take the name of a system reason
    When I POST to /secured/stock-movement-reasons with payload:
      """json
      {"name": "count discrepancy", "movementTypes": ["STOCK_ADJUSTMENT_INCREASE"]}
      """
    Then the response status should be 400
    And the response error message should be "A movement reason with the name 'count discrepancy' already exists"
    And Stock_Movement_Reason table should have exactly 5 records in database

  Scenario: Renaming a reason frees its old name
    Given a stock movement reason named "Packaging" exists
    When I PUT the stock movement reason named "Packaging" with payload:
      """json
      {"name": "Shipping Pack"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Shipping Pack"}
      """
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Shipping Pack"}
      """
    And Stock_Movement_Reason should not match example:
      """json
      {"name": "Packaging"}
      """
    When I POST to /secured/stock-movement-reasons with payload:
      """json
      {"name": "Packaging", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE"]}
      """
    Then the response status should be 200
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Packaging"}
      """
    And Stock_Movement_Reason table should have exactly 7 records in database

  Scenario: An update that only carries a description keeps the name and movement types
    Given a stock movement reason named "Packaging" exists
    When I PUT the stock movement reason named "Packaging" with payload:
      """json
      {"description": "Stock used as packaging"}
      """
    Then the response status should be 200
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Packaging", "description": "Stock used as packaging", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE"]}
      """

  Scenario: A null description clears the reason's description
    When I POST to /secured/stock-movement-reasons with payload:
      """json
      {"name": "Packaging", "description": "Temporary", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE"]}
      """
    Then the response status should be 200
    When I PUT the stock movement reason named "Packaging" with payload:
      """json
      {"description": null}
      """
    Then the response status should be 200
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Packaging", "description": null}
      """

  Scenario: A reason's movement types can be changed
    Given a stock movement reason named "Packaging" exists
    When I PUT the stock movement reason named "Packaging" with payload:
      """json
      {"movementTypes": ["STOCK_ADJUSTMENT_INCREASE"]}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Packaging", "movementTypes": ["STOCK_ADJUSTMENT_INCREASE"]}
      """
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Packaging", "movementTypes": ["STOCK_ADJUSTMENT_INCREASE"]}
      """
    And Stock_Movement_Reason should not match example:
      """json
      {"name": "Packaging", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE"]}
      """

  Scenario: A reason keeps at least one movement type on update
    Given a stock movement reason named "Packaging" exists
    When I PUT the stock movement reason named "Packaging" with payload:
      """json
      {"movementTypes": []}
      """
    Then the response status should be 400
    And the response error message should be "A movement reason must apply to at least one movement type"
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Packaging", "movementTypes": ["STOCK_ADJUSTMENT_DECREASE"]}
      """

  Scenario Outline: A reason cannot be renamed to a name another reason holds
    Given a stock movement reason named "Packaging" exists
    And a stock movement reason named "Shipping Pack" exists
    When I PUT the stock movement reason named "Packaging" with payload:
      """json
      {"name": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A movement reason with the name '<name>' already exists"
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Packaging"}
      """

    Examples:
      | name           |
      | shipping pack  |
      | Damaged        |

  Scenario Outline: A system reason cannot be changed
    When I PUT the stock movement reason named "Damaged" with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "System-defined reasons cannot be modified"
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Damaged", "description": "Stock lost or written off due to physical damage", "systemDefined": true}
      """

    Examples:
      | payload                                            |
      | {"name": "Damaged Goods"}                          |
      | {"description": "Edited"}                          |
      | {"movementTypes": ["STOCK_ADJUSTMENT_INCREASE"]}   |

  Scenario: Updating a reason that does not exist is rejected
    When I PUT to /secured/stock-movement-reasons with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "name": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Stock_Movement_Reason table should have exactly 5 records in database

  Scenario: Deleting an organization reason removes it
    Given a stock movement reason named "Packaging" exists
    When I DELETE the stock movement reason named "Packaging"
    Then the response status should be 204
    When I GET to /secured/stock-movement-reasons
    Then response contains details:
      """json
      [
        {"code": "DMG", "name": "Damaged"},
        {"code": "QI", "name": "Quality Issue"},
        {"code": "THF", "name": "Theft"},
        {"code": "CD", "name": "Count Discrepancy"},
        {"code": "EXP", "name": "Expired"}
      ]
      """
    And Stock_Movement_Reason should not match example:
      """json
      {"name": "Packaging"}
      """

  Scenario: A system reason cannot be deleted
    When I DELETE the stock movement reason named "Damaged"
    Then the response status should be 400
    And the response error message should be "System-defined reasons cannot be deleted"
    And Stock_Movement_Reason should match example:
      """json
      {"name": "Damaged", "systemDefined": true}
      """
    And Stock_Movement_Reason table should have exactly 5 records in database

  Scenario: Deleting an unknown reason succeeds without changing anything
    When I DELETE to /secured/stock-movement-reasons/00000000-0000-0000-0000-000000000001
    Then the response status should be 204
    And Stock_Movement_Reason table should have exactly 5 records in database
