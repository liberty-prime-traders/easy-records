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
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 100.00}]
      }
      """
    Then the response status should be 200
    And response list size is 1
    And response contains details:
      """json
      {
        "expenseTypeName": "Rent",
        "payeeDisplayName": "City Water",
        "amount": 100,
        "sourceType": "ADHOC",
        "batchDescription": "March rent",
        "status": "UNPAID",
        "amountPaid": 0,
        "balanceRemaining": 100,
        "voided": false
      }
      """
    And account "005.003" should have a balance of "100.00"
    And account "002.001.001" should have a balance of "100.00"
    And OrgExpense table should have exactly 1 records in database

  Scenario: Several rows in one batch each record their own amount and post their own balance
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "Quarterly rent",
        "expenseDate": "#today",
        "rows": [
          {"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 60.00},
          {"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 40.50, "description": "Deposit"}
        ]
      }
      """
    Then the response status should be 200
    And response list size is 2
    And response contains details:
      """json
      {"description": "Deposit", "amount": 40.5, "status": "UNPAID"}
      """
    And account "005.003" should have a balance of "100.50"
    And account "002.001.001" should have a balance of "100.50"
    And OrgExpense table should have exactly 2 records in database

  Scenario Outline: A standalone expense whose amount is not positive is rejected and nothing is saved
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": <amount>}]
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
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 100.00}]
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
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 100.00}]
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
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 100.00}]
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
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 100.00}]
      }
      """
    And I void the recorded expense with reason "Entered twice"
    Then the response status should be 200
    And response contains details:
      """json
      {"voided": true, "voidReason": "Entered twice"}
      """
    And account "005.003" should have a balance of "0.00"
    And account "002.001.001" should have a balance of "0.00"
    And OrgExpenseVoid table should have exactly 1 records in database

  Scenario: An expense that has already been voided cannot be voided again
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 100.00}]
      }
      """
    And I void the recorded expense with reason "Entered twice"
    And I void the recorded expense with reason "Entered twice"
    Then the response status should be 400
    And the response error message should be "Expense #recordedExpenseReference has been voided" for the recorded expense
    And OrgExpenseVoid table should have exactly 1 records in database

  Scenario: Voiding an expense without a reason is rejected
    When I record a standalone expense batch with payload:
      """json
      {
        "description": "March rent",
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 100.00}]
      }
      """
    And I void the recorded expense with reason ""
    Then the response status should be 400
    And the response error message should be "A void reason is required"
    And OrgExpenseVoid table should have no records in database
    And account "005.003" should have a balance of "100.00"

  Scenario: Voiding an expense reference that does not exist is rejected
    When I POST to/from /secured/org-expenses/void with payload:
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
        "expenseDate": "#today",
        "rows": [{"payeeContactId": "#contact", "expenseTypeId": "#selectedExpenseType", "amount": 100.00}]
      }
      """
    And I get the recorded expense
    Then the response status should be 200
    And response contains details:
      """json
      {"expenseTypeName": "Rent", "payeeDisplayName": "City Water", "status": "UNPAID", "voided": false}
      """

  Scenario: Reading an expense reference that does not exist is rejected
    When I GET to/from /secured/org-expenses/EX999999
    Then the response status should be 400
    And the response error message should be "Expense EX999999 not found"
