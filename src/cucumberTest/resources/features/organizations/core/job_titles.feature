Feature: Job titles
  As an authenticated organization user
  I want to keep the job titles my people can hold
  So that every title is recorded once, with a value

  Scenario: Creating a job title stores it and lists it
    When I POST to /secured/job-title with payload:
      """json
      {"value": "Cashier"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"value": "Cashier"}
      """
    When I GET to /secured/job-title
    Then response contains details:
      """json
      [{"value": "Cashier"}]
      """
    And Job_Title table should contain exactly these records:
      """json
      [{"value": "Cashier"}]
      """

  Scenario Outline: A job title needs a value
    When I POST to /secured/job-title with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "A job title must have a value"
    When I GET to /secured/job-title
    Then response contains details:
      """json
      []
      """
    And Job_Title table should have no records in database

    Examples:
      | payload           |
      | {"value": "   "}  |
      | {}                |

  Scenario Outline: A job title is unique ignoring case and spacing
    When I POST to /secured/job-title with payload:
      """json
      {"value": "Cashier"}
      """
    Then the response status should be 200
    When I POST to /secured/job-title with payload:
      """json
      {"value": "<value>"}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    And Job_Title table should contain exactly these records:
      """json
      [{"value": "Cashier"}]
      """

    Examples:
      | value     | message                                              |
      | CASHIER   | A job title with the value CASHIER already exists.   |
      | Cash ier  | A job title with the value Cash ier already exists.  |
