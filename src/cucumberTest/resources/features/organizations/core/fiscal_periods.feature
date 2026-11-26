Feature: Fiscal periods
  As an authenticated organization user
  I want to name and close the fiscal periods my organization posts into
  So that activity is only closed off from a period that has ended

  Scenario: The period covering today is listed as current and not closable
    When I GET to /secured/fiscal-periods
    Then the response status should be 200
    And response contains item with details:
      """json
      {"current": true, "closable": false}
      """

  Scenario: Year-end closing the period that covers today is refused
    Given the open fiscal period is named "Trial Period"
    And the open fiscal period is not a year-end period
    # The year-end endpoint refuses a regular period before it checks the ended-date rule.
    When I year-end close the open fiscal period
    Then the response status should be 400
    And the response error message should be "Period 'Trial Period' is not a year-end period"
    And Fiscal_Period should match example:
      """json
      {"name": "Trial Period", "closedAt": null}
      """

  Scenario: Renaming the period covering today is saved
    When I rename the open fiscal period to "Trial Period"
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Trial Period", "current": true}
      """
    And Fiscal_Period should match example:
      """json
      {"name": "Trial Period"}
      """
