Feature: Payment methods
  As an authenticated organization user
  I want to manage the payment methods my sales are taken through
  So that every payment method is linked to an asset account and has a unique name

  Scenario: Creating a payment method links it to its asset account
    When I POST to /secured/payment-methods with payload:
      """json
      {"name": "Till", "description": "Counter cash", "accountCode": "001.001.001"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Till", "description": "Counter cash", "accountCode": "001.001.001", "linkedAccount": "Cash (1.1.1)"}
      """
    When I GET to /secured/payment-methods
    Then response contains item with details:
      """json
      {"name": "Till", "accountCode": "001.001.001", "linkedAccount": "Cash (1.1.1)"}
      """
    And Payment_Method should match example:
      """json
      {"name": "Till", "description": "Counter cash", "accountCode": "001.001.001"}
      """

  Scenario Outline: A payment method needs a name and an account
    When I POST to /secured/payment-methods with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    When I GET to /secured/payment-methods
    Then response list size is 0
    And Payment_Method table should have no records in database

    Examples:
      | payload                                        | message                              |
      | {"name": "", "accountCode": "001.001.001"}     | A Payment Method must have a name   |
      | {"name": "   ", "accountCode": "001.001.001"}  | A Payment Method must have a name   |
      | {"accountCode": "001.001.001"}                 | A Payment Method must have a name   |
      | {"name": "Till", "accountCode": ""}            | A Payment Method must have an account |
      | {"name": "Till", "accountCode": "   "}         | A Payment Method must have an account |
      | {"name": "Till"}                               | The request body could not be read   |

  Scenario Outline: A payment method account must be a selectable leaf asset
    When I POST to /secured/payment-methods with payload:
      """json
      {"name": "Till", "accountCode": "<accountCode>"}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    When I GET to /secured/payment-methods
    Then response list size is 0
    And Payment_Method table should have no records in database

    Examples:
      | accountCode | message                                                                      |
      | 999.999     | Payment method account not found: 999.999                                    |
      | 002.004     | Payment method account must be of type Asset                                 |
      | 001         | Assets (1) has child accounts; Payment method account must be a leaf account |

  Scenario Outline: Payment method names are unique on their letters with spaces and case ignored
    Given a payment method named "Till" exists
    When I POST to /secured/payment-methods with payload:
      """json
      {"name": "<name>", "accountCode": "001.001.001"}
      """
    Then the response status should be 400
    And the response error message should be "A Payment Method using the name '<shownName>' already exists"
    When I GET to /secured/payment-methods
    Then response list size is 1
    And Payment_Method table should have exactly 1 records in database

    Examples:
      | name    | shownName |
      | till    | till      |
      | TILL    | TILL      |
      | T i l l | T i l l   |
      | Till    | Till      |

  Scenario: A payment method can be renamed to another case of its own name
    Given a payment method named "Till" exists
    When I PUT the payment method with payload:
      """json
      {"id": "#paymentMethod", "name": "TILL", "accountCode": "001.001.001"}
      """
    Then the response status should be 200
    And Payment_Method should match example:
      """json
      {"name": "TILL", "accountCode": "001.001.001"}
      """
    And Payment_Method table should have exactly 1 records in database

  Scenario: Renaming a payment method frees its old name
    Given a payment method named "Till" exists
    When I PUT the payment method with payload:
      """json
      {"id": "#paymentMethod", "name": "Counter", "accountCode": "001.001.001"}
      """
    Then the response status should be 200
    And Payment_Method should match example:
      """json
      {"name": "Counter"}
      """
    When I POST to /secured/payment-methods with payload:
      """json
      {"name": "Till", "accountCode": "001.001.001"}
      """
    Then the response status should be 200
    And Payment_Method table should have exactly 2 records in database

  Scenario: A payment method cannot take the name of another payment method
    Given a payment method named "Card" exists
    And a payment method named "Till" exists
    When I PUT the payment method with payload:
      """json
      {"id": "#paymentMethod", "name": "card", "accountCode": "001.001.001"}
      """
    Then the response status should be 400
    And the response error message should be "A Payment Method using the name 'card' already exists"
    And Payment_Method should match example:
      """json
      {"name": "Till"}
      """
    And Payment_Method table should have exactly 2 records in database

  Scenario: Renaming without an account keeps the linked account
    Given a payment method named "Till" exists
    When I PUT the payment method with payload:
      """json
      {"id": "#paymentMethod", "name": "Counter"}
      """
    Then the response status should be 200
    And Payment_Method should match example:
      """json
      {"name": "Counter", "accountCode": "001.001.001"}
      """
    And Payment_Method table should have exactly 1 records in database

  Scenario: A null description clears the payment method's description
    Given a payment method named "Till" exists
    And I PUT the payment method with payload:
      """json
      {"id": "#paymentMethod", "name": "Till", "description": "Temporary"}
      """
    When I PUT the payment method with payload:
      """json
      {"id": "#paymentMethod", "name": "Till", "description": null}
      """
    Then the response status should be 200
    And Payment_Method should match example:
      """json
      {"name": "Till", "description": null}
      """
    And Payment_Method table should have exactly 1 records in database

  Scenario: An update cannot point the payment method at an account that does not exist
    Given a payment method named "Till" exists
    When I PUT the payment method with payload:
      """json
      {"id": "#paymentMethod", "name": "Till", "accountCode": "999.999"}
      """
    Then the response status should be 400
    And the response error message should be "Payment method account not found: 999.999"
    And Payment_Method should match example:
      """json
      {"name": "Till", "accountCode": "001.001.001"}
      """
    And Payment_Method table should have exactly 1 records in database

  Scenario: An update cannot clear the account
    Given a payment method named "Till" exists
    When I PUT the payment method with payload:
      """json
      {"id": "#paymentMethod", "name": "Till", "accountCode": ""}
      """
    Then the response status should be 400
    And the response error message should be "A Payment Method must have an account"
    And Payment_Method should match example:
      """json
      {"name": "Till", "accountCode": "001.001.001"}
      """
    And Payment_Method table should have exactly 1 records in database
    And Payment_Method table should have exactly 1 records in database

  Scenario: An update without an id is rejected
    When I PUT to/from /secured/payment-methods with payload:
      """json
      {"name": "Till", "accountCode": "001.001.001"}
      """
    Then the response status should be 400
    And the response error message should be "Queried Using Empty Id"
    When I GET to /secured/payment-methods
    Then response list size is 0
    And Payment_Method table should have no records in database

  Scenario: An update to a payment method that does not exist is rejected
    When I PUT to/from /secured/payment-methods with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "name": "Till", "accountCode": "001.001.001"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    When I GET to /secured/payment-methods
    Then response list size is 0
    And Payment_Method table should have no records in database

  Scenario: Deleting a payment method removes it and keeps the others
    Given a payment method named "Card" exists
    And a payment method named "Till" exists
    When I DELETE the payment method
    Then the response status should be 204
    When I GET to /secured/payment-methods
    Then response list size is 1
    And Payment_Method should match example:
      """json
      {"name": "Card"}
      """
    And Payment_Method table should have exactly 1 records in database
