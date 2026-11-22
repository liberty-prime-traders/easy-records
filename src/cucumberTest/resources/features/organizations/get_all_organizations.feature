Feature: Get All Organizations
  As a platform administrator
  I want to retrieve all organizations
  So that I can view all tenants in the system

  @smoke
  Scenario: Platform administrator can retrieve all organizations
    Given I am authenticated as a platform admin
    When I get all organizations
    Then the response status should be 200
    And the response should contain 1 items
    And response contains details:
      """
      {"id": "#organization"}
      """

  @smoke
  Scenario: Regular organization user cannot retrieve all organizations (403)
    When I get all organizations
    Then the response status should be 403

  @smoke
  Scenario: Unauthenticated user cannot retrieve all organizations (403)
    Given I am not authenticated
    When I get all organizations
    Then the response status should be 403
