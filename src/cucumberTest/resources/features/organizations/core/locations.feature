Feature: Locations
  As an organization user
  I want to see the organization's locations and keep their names and timezones valid
  So that every location can be told apart and runs on its own timezone

  # Locations are protected from the reset, so no scenario here creates one: a created location outlives the scenario.

  Scenario: Members can list the organization's locations
    When I GET to /secured/locations
    Then the response status should be 200
    And response contains item with details:
      """json
      {"name": "Test Location", "locationType": "SHOP"}
      """

  Scenario: A regular organization user cannot create a location (403)
    Given I am authenticated as an organization user
    When I POST to /secured/locations with payload:
      """json
      {"name": "Remote Depot", "description": "Not stored"}
      """
    Then the response status should be 403
    When I GET to /secured/locations
    Then response contains no item with details:
      """json
      {"description": "Not stored"}
      """

  Scenario Outline: A location needs a name
    Given I am authenticated as a platform admin
    When I POST to /secured/locations with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "A location must have a name"
    When I GET to /secured/locations
    Then response contains no item with details:
      """json
      {"description": "Not stored"}
      """

    Examples:
      | payload                                               |
      | {"name": "", "description": "Not stored"}             |
      | {"name": "   ", "description": "Not stored"}          |
      | {"locationType": "SHOP", "description": "Not stored"} |

  Scenario: A location type outside the known types is rejected
    Given I am authenticated as a platform admin
    When I POST to /secured/locations with payload:
      """json
      {"name": "Remote Depot", "locationType": "KIOSK", "description": "Not stored"}
      """
    Then the response status should be 400
    And the response error message should be "The request body could not be read"
    When I GET to /secured/locations
    Then response contains no item with details:
      """json
      {"description": "Not stored"}
      """

  Scenario Outline: Location names are unique ignoring case and spaces
    Given I am authenticated as a platform admin
    When I POST to /secured/locations with payload:
      """json
      {"name": "<name>", "description": "Not stored"}
      """
    Then the response status should be 400
    And the response error message should be "A location with the name <name> already exists"
    When I GET to /secured/locations
    Then response contains no item with details:
      """json
      {"description": "Not stored"}
      """

    Examples:
      | name          |
      | Test Location |
      | test location |
      | TestLocation  |

  Scenario: A location needs a valid timezone
    Given I am authenticated as a platform admin
    When I POST to /secured/locations with payload:
      """json
      {"name": "Remote Depot", "timezone": "Mars/Olympus", "description": "Not stored"}
      """
    Then the response status should be 400
    And the response error message should be "'Mars/Olympus' is not a valid timezone"
    When I GET to /secured/locations
    Then response contains no item with details:
      """json
      {"name": "Remote Depot"}
      """

  Scenario Outline: An update must carry a name
    Given I am authenticated as a platform admin
    When I PUT to /secured/locations with payload:
      """json
      <payload>
      """
    Then the response status should be 400
    And the response error message should be "A location must have a name"
    And Location should exist in database with id #location and options:
      """json
      {"name": "Test Location", "description": null, "timezone": "Africa/Addis_Ababa"}
      """

    Examples:
      | payload                                      |
      | {"name": ""}                                 |
      | {"name": "   ", "description": "Not stored"} |
      | {"description": "Not stored"}                |

  Scenario: An update must keep a valid timezone
    Given I am authenticated as a platform admin
    When I PUT to /secured/locations with payload:
      """json
      {"name": "Test Location", "timezone": "Mars/Olympus"}
      """
    Then the response status should be 400
    And the response error message should be "'Mars/Olympus' is not a valid timezone"
    And Location should exist in database with id #location and options:
      """json
      {"name": "Test Location", "timezone": "Africa/Addis_Ababa"}
      """

  Scenario: An update may repeat the location's own name
    Given I am authenticated as a platform admin
    When I PUT to /secured/locations with payload:
      """json
      {"name": "Test Location", "locationType": "SHOP", "timezone": "Africa/Addis_Ababa"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"name": "Test Location", "locationType": "SHOP"}
      """
    And Location should exist in database with id #location and options:
      """json
      {"name": "Test Location", "timezone": "Africa/Addis_Ababa"}
      """
