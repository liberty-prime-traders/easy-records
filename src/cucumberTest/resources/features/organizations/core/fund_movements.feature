Feature: Recording owner capital, owner draws and voiding fund movements
  As an authenticated organization user
  I want to record money the owner puts into or takes out of the business
  So that cash and owner's equity show it, and a wrong movement can be reversed

  Scenario: Owner capital is saved and raises cash and owner's capital
    When I POST to /secured/fund-movements/capital with payload:
      """json
      {"accountCode": "001.001.001", "amount": 100.0, "movementDate": "{{today}}", "note": "Owner injects cash"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "type": "CONTRIBUTE_CAPITAL_TO_BUSINESS",
        "debitAccount": {"code": "001.001.001", "label": "Cash (1.1.1)"},
        "creditAccount": {"code": "003.001", "label": "Owner's Capital (3.1)"},
        "amount": 100
      }
      """
    And Fund_Movement table should contain exactly these records:
      """json
      [{"debitAccountCode": "001.001.001", "creditAccountCode": "003.001", "amount": 100}]
      """
    And account "001.001.001" should have a balance of "100.00"
    And account "003.001" should have a balance of "100.00"

  Scenario: An owner draw takes cash out and debits owner's draws
    Given the owner has contributed "100.00" of cash
    When I POST to /secured/fund-movements/draw with payload:
      """json
      {"accountCode": "001.001.001", "amount": 40.0, "movementDate": "{{today}}", "note": "Personal groceries"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {
        "type": "WITHDRAW_FOR_PERSONAL_USE",
        "debitAccount": {"code": "003.003", "label": "Owner's Draws (3.3)"},
        "creditAccount": {"code": "001.001.001", "label": "Cash (1.1.1)"},
        "amount": 40
      }
      """
    And Fund_Movement table should contain exactly these records:
      """json
      [
        {"debitAccountCode": "001.001.001", "creditAccountCode": "003.001", "amount": 100},
        {"debitAccountCode": "003.003", "creditAccountCode": "001.001.001", "amount": 40}
      ]
      """
    And account "003.003" should have a balance of "40.00"
    And account "001.001.001" should have a balance of "60.00"

  Scenario: Voiding a capital contribution reverses it and keeps the movement
    Given the owner has contributed "100.00" of cash
    When I void the owner's fund movement with reason "Entered twice"
    Then the response status should be 200
    And response contains details:
      """json
      {
        "type": "CONTRIBUTE_CAPITAL_TO_BUSINESS",
        "debitAccount": {"label": "Cash (1.1.1)"},
        "creditAccount": {"label": "Owner's Capital (3.1)"},
        "amount": 100,
        "void": {"reason": "Entered twice"}
      }
      """
    And Fund_Movement_Void table should contain exactly these records:
      """json
      [{"reason": "Entered twice"}]
      """
    And Fund_Movement table should contain exactly these records:
      """json
      [{"debitAccountCode": "001.001.001", "creditAccountCode": "003.001", "amount": 100}]
      """
    And account "001.001.001" should have a balance of "0.00"
    And account "003.001" should have a balance of "0.00"

  Scenario: Voiding a movement that does not exist is rejected
    When I POST to /secured/fund-movements/void with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000000", "reason": "Wrong id"}
      """
    Then the response status should be 400
    And the response error message should be "Fund movement not found"
    And Fund_Movement_Void table should have no records in database

  Scenario Outline: A movement into an account that cannot hold cash is rejected
    When I POST to /secured/fund-movements/capital with payload:
      """json
      {"accountCode": "<account>", "amount": 100.0, "movementDate": "{{today}}"}
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And Fund_Movement table should have no records in database

    Examples:
      | account     | error                                                                              |
      | 002.004     | Fund movement account must be of type Asset                                        |
      | 001.001.003 | System-defined fund movement account must be a direct child of Digital Payments    |
      | 009.999.999 | Fund movement account not found: 009.999.999                                       |

  Scenario Outline: A movement whose amount is not positive is rejected
    When I POST to /secured/fund-movements/capital with payload:
      """json
      {"accountCode": "001.001.001", "amount": <amount>, "movementDate": "{{today}}"}
      """
    Then the response status should be 400
    And the response error message should be "<error>"
    And Fund_Movement table should have no records in database

    Examples:
      | amount | error                            |
      | 0.00   | Amount must be greater than zero |
      | -5.00  | Amount must not be negative      |

  Scenario: A movement dated outside every open fiscal period leaves nothing behind
    When I POST to /secured/fund-movements/capital with payload:
      """json
      {"accountCode": "001.001.001", "amount": 100.0, "movementDate": "2001-01-15"}
      """
    Then the response status should be 400
    And the response error message should be "No open fiscal period for 2001-01-15"
    And Fund_Movement table should have no records in database
    And account "001.001.001" should have a balance of "0.00"
    And account "003.001" should have a balance of "0.00"
