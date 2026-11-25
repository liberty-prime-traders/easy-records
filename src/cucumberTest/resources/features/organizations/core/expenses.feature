@consumes-from-kafka
Feature: Organization expenses
  As an authenticated organization user
  I want to record the money the organization spends outside any one location
  So that the expense account and the payable to the payee show what was incurred

  Background:
    Given a service provider named "City Water" exists
    And the expense type "Rent" is selected

  Scenario: A standalone expense posts to its expense account and to trade payables
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 100.0}]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      [{"expenseTypeName": "Rent", "batchDescription": "March rent", "amount": 100, "status": "UNPAID"}]
      """
    And account "005.003" should have a balance of "100.00"
    And account "002.001.001" should have a balance of "100.00"
    And OrgExpense table should contain exactly these records:
      """json
      [{"expenseAccountCode": "005.003", "amount": "100.00"}]
      """

  Scenario: Several rows in one batch each record their own amount and post their own balance
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "Quarterly rent",
        "expenseDate": "{{today}}",
        "rows": [
          {"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 60.0},
          {
            "payeeContactId": "{{contact}}",
            "expenseTypeId": "{{selectedExpenseType}}",
            "amount": 40.5,
            "description": "Deposit"
          }
        ]
      }
      """
    Then the response status should be 200
    And response contains details:
      """json
      [
        {"batchDescription": "Quarterly rent", "amount": 60.0, "balanceRemaining": 60.0},
        {"batchDescription": "Quarterly rent", "amount": 40.5, "balanceRemaining": 40.5}
      ]
      """
    And account "005.003" should have a balance of "100.50"
    And account "002.001.001" should have a balance of "100.50"
    And OrgExpense table should contain exactly these records:
      """json
      [{"expenseAccountCode": "005.003", "amount": "60.00"}, {"expenseAccountCode": "005.003", "amount": "40.50"}]
      """

  Scenario Outline: A standalone expense whose amount is not positive is rejected and nothing is saved
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": <amount>}]
      }
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And OrgExpense table should have no records in database
    And account "005.003" should have a balance of "0.00"

    Examples:
      | amount | error                              |
      | 0.00   | Amount must be greater than zero   |
      | -5.00  | Amount must not be negative        |

  Scenario: A standalone expense without a batch description is rejected
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "   ",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 100.0}]
      }
      """
    Then the response status should be 400
    And the response error message should be "Batch description is required"
    And OrgExpense table should have no records in database

  Scenario: A payee whose contact types do not list the expense type is rejected
    Given a customer named "Plain Customer" exists
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 100.0}]
      }
      """
    Then the response status should be 400
    And the response error message should be "The selected payee (Plain Customer) is not eligible for expense type: Rent"
    And OrgExpense table should have no records in database

  Scenario: An expense type that is not offered on the standalone screen is rejected
    Given the expense type "Wages" is selected
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March wages",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 100.0}]
      }
      """
    Then the response status should be 400
    And the response error message should be "Expense type Wages cannot be used from this screen"
    And OrgExpense table should have no records in database

  Scenario: A recorded expense is voided and its posting is reversed
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 100.0}]
      }
      """
    And I void the recorded expense with reason "Entered twice"
    Then the response status should be 200
    And response contains details:
      """json
      {"batchDescription": "March rent", "amount": 100, "voided": true, "voidReason": "Entered twice"}
      """
    And account "005.003" should have a balance of "0.00"
    And account "002.001.001" should have a balance of "0.00"
    And OrgExpenseVoid table should contain exactly these records:
      """json
      [{"reason": "Entered twice"}]
      """

  Scenario: An expense that has already been voided cannot be voided again
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 100.0}]
      }
      """
    And I void the recorded expense with reason "Entered twice"
    And I void the recorded expense with reason "Entered twice"
    Then the response status should be 400
    And the response error message should be "Expense {{recordedExpenseReference}} has been voided" for the recorded expense
    And OrgExpenseVoid table should contain exactly these records:
      """json
      [{"reason": "Entered twice"}]
      """

  Scenario: Voiding an expense without a reason is rejected
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 100.0}]
      }
      """
    And I void the recorded expense with reason ""
    Then the response status should be 400
    And the response error message should be "A void reason is required"
    And OrgExpenseVoid table should have no records in database
    And account "005.003" should have a balance of "100.00"

  Scenario: Voiding an expense reference that does not exist is rejected
    When I POST to /secured/org-expenses/void with payload:
      """json
      {"expenseReference": "EX999999", "reason": "Entered twice"}
      """
    Then the response status should be 400
    And the response error message should be "Expense EX999999 not found"
    And OrgExpenseVoid table should have no records in database

  Scenario: A recorded expense is read back by its reference
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "{{today}}",
        "rows": [{"payeeContactId": "{{contact}}", "expenseTypeId": "{{selectedExpenseType}}", "amount": 100.0}]
      }
      """
    And I get the recorded expense
    Then the response status should be 200
    And response contains details:
      """json
      {"batchDescription": "March rent", "amount": 100}
      """
    And OrgExpense table should contain exactly these records:
      """json
      [{"expenseAccountCode": "005.003", "amount": "100.00"}]
      """

  Scenario: Reading an expense reference that does not exist is rejected
    When I GET to /secured/org-expenses/EX999999
    Then the response status should be 400
    And the response error message should be "Expense EX999999 not found"
