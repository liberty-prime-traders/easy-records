Feature: Contacts
  As an authenticated organization user
  I want to keep customers, suppliers and people in my contact list
  So that every contact has a unique name, valid types and a status I can rely on

  Scenario: Creating an organization contact stores it and lists it with its type
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["CUSTOMER"], "identityType": "ORGANIZATION", "companyName": "Acme Traders",
       "email": "orders@acme.example", "phone": "0712345678", "creditLimit": 1500, "notes": "Pays on delivery"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"companyName": "Acme Traders", "fullName": "Acme Traders", "identityType": "ORGANIZATION",
       "contactTypes": ["CUSTOMER"], "email": "orders@acme.example", "creditLimit": 1500.00,
       "status": "ACTIVE", "systemDefined": false}
      """
    When I GET to /secured/contacts
    Then response contains item with details:
      """json
      {"companyName": "Acme Traders", "contactTypes": ["CUSTOMER"]}
      """
    And Contact should match example:
      """json
      {"companyName": "Acme Traders", "contactTypes": ["CUSTOMER"], "email": "orders@acme.example",
       "notes": "Pays on delivery", "status": "ACTIVE"}
      """
    And Contact table should have exactly 2 records in database

  Scenario: Creating a person stores the first and last name as the full name
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["SUPPLIER", "EMPLOYEE"], "identityType": "INDIVIDUAL", "firstName": "Ann", "lastName": "Lee"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"firstName": "Ann", "lastName": "Lee", "fullName": "Ann Lee", "identityType": "INDIVIDUAL",
       "contactTypes": ["SUPPLIER", "EMPLOYEE"]}
      """
    And Contact should match example:
      """json
      {"firstName": "Ann", "lastName": "Lee", "companyName": null, "contactTypes": ["SUPPLIER", "EMPLOYEE"]}
      """
    And Contact table should have exactly 2 records in database

  Scenario: An organization keeps only its company name when a person's names are sent with it
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["SUPPLIER"], "identityType": "ORGANIZATION", "companyName": "Beta Ltd",
       "firstName": "Ignored", "lastName": "Ignored"}
      """
    Then the response status should be 200
    And Contact should match example:
      """json
      {"companyName": "Beta Ltd", "firstName": null, "lastName": null}
      """
    And Contact table should have exactly 2 records in database

  Scenario: A contact can be created as suspended
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["SUPPLIER"], "identityType": "ORGANIZATION", "companyName": "Beta Ltd", "status": "SUSPENDED"}
      """
    Then the response status should be 200
    And response contains details:
      """json
      {"companyName": "Beta Ltd", "status": "SUSPENDED"}
      """
    And Contact should match example:
      """json
      {"companyName": "Beta Ltd", "status": "SUSPENDED"}
      """
    And Contact table should have exactly 2 records in database

  Scenario Outline: A contact needs one contact type on its own
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": <contactTypes>, "identityType": "ORGANIZATION", "companyName": "Acme Traders"}
      """
    Then the response status should be 400
    And the response error message should be "<message>"
    When I GET to /secured/contacts
    Then response list size is 1
    And Contact table should have exactly 1 records in database

    Examples:
      | contactTypes            | message                                                  |
      | []                      | At least one contact type required                       |
      | ["OTHER", "CUSTOMER"]   | OTHER cannot be combined with other contact types        |

  Scenario: An organization without a company name is rejected
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["CUSTOMER"], "identityType": "ORGANIZATION", "firstName": "Ann"}
      """
    Then the response status should be 400
    And the response error message should be "Identity type ORGANIZATION requires company name"
    And Contact table should have exactly 1 records in database

  Scenario: A person without a first name is rejected
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["CUSTOMER"], "identityType": "INDIVIDUAL", "companyName": "Acme Traders"}
      """
    Then the response status should be 400
    And the response error message should be "Identity type INDIVIDUAL requires first name"
    And Contact table should have exactly 1 records in database

  Scenario: A person with no name at all is rejected as an unreadable body
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["CUSTOMER"], "identityType": "INDIVIDUAL"}
      """
    Then the response status should be 400
    And the response error message should be "The request body could not be read"
    And Contact table should have exactly 1 records in database

  Scenario Outline: A company name is unique ignoring case and spacing
    Given a customer named "Acme Traders" exists
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["SUPPLIER"], "identityType": "ORGANIZATION", "companyName": "<name>"}
      """
    Then the response status should be 400
    And the response error message should be "A Contact with company '<name>' already exists"
    Then Contact table should have exactly 2 records in database

    Examples:
      | name           |
      | ACME TRADERS   |
      | Acme  Traders  |

  Scenario: A person and a company cannot share a name
    Given a customer named "Ann Lee" exists
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["CUSTOMER"], "identityType": "INDIVIDUAL", "firstName": "Ann", "lastName": "Lee"}
      """
    Then the response status should be 400
    And the response error message should be "A Contact with person 'Ann Lee' already exists"
    Then Contact table should have exactly 2 records in database

  Scenario: Names that differ by punctuation are different companies
    Given a customer named "Acme Traders" exists
    When I POST to /secured/contacts with payload:
      """json
      {"contactTypes": ["CUSTOMER"], "identityType": "ORGANIZATION", "companyName": "Acme-Traders"}
      """
    Then the response status should be 200
    And Contact table should have exactly 3 records in database

  Scenario: A bulk import creates every row with its own types and identity
    When I POST to /secured/contacts/bulk with payload:
      """json
      [
        {"contactTypes": "CUS,SUP", "companyName": "Acme Traders"},
        {"contactTypes": "cus", "firstName": "Ann", "lastName": "Lee"}
      ]
      """
    Then the response status should be 200
    And response list size is 2
    And response contains details with exact lists in order:
      """json
      [
        {"companyName": "Acme Traders", "identityType": "ORGANIZATION", "contactTypes": ["CUSTOMER", "SUPPLIER"]},
        {"fullName": "Ann Lee", "identityType": "INDIVIDUAL", "contactTypes": ["CUSTOMER"]}
      ]
      """
    And Contact table should have exactly 3 records in database

  Scenario: A bulk import reports every bad row and saves none of them
    Given a customer named "Acme Traders" exists
    When I POST to /secured/contacts/bulk with payload:
      """json
      [
        {"contactTypes": "CUS", "companyName": "ACME traders"},
        {"contactTypes": "XYZ", "companyName": "Beta Ltd"},
        {"contactTypes": "CUS", "firstName": "  "},
        {"contactTypes": "CUS", "companyName": "Gamma Co"},
        {"contactTypes": "CUS", "companyName": "gamma co"},
        {"companyName": "Delta"}
      ]
      """
    Then the response status should be 400
    And the response error message should be "Bulk contact import validation failed"
    And response contains details with exact lists in order:
      """json
      {"body": [
        "Row 1: A Contact with company 'ACME traders' already exists",
        "Row 2: Unknown contact type 'XYZ'",
        "Row 3: Identity type INDIVIDUAL requires first name",
        "Row 5: A Contact with company 'gamma co' appears more than once in the request",
        "Row 6: At least one contact type required"
      ]}
      """
    And Contact table should have exactly 2 records in database

  Scenario: The walk-in customer is listed as system defined
    When I GET to /secured/contacts
    Then response list size is 1
    And response contains item with details:
      """json
      {"fullName": "Walk-In Customer", "identityType": "INDIVIDUAL", "systemDefined": true}
      """

  Scenario: The walk-in customer cannot be modified
    When I PUT to /secured/contacts with payload:
      """json
      {"id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "notes": "Changed"}
      """
    Then the response status should be 400
    And the response error message should be "System-defined contacts cannot be modified"
    And Contact should match example:
      """json
      {"firstName": "Walk-In Customer", "notes": null, "systemDefined": true}
      """
    And Contact table should have exactly 1 records in database

  Scenario: An update that only carries an email keeps the other fields
    Given a customer named "Acme Traders" exists
    When I PUT to /secured/contacts with payload:
      """json
      {"id": "#contact", "email": "sales@acme.example"}
      """
    Then the response status should be 200
    And Contact should exist in database with id #contact and options:
      """json
      {"companyName": "Acme Traders", "email": "sales@acme.example", "contactTypes": ["CUSTOMER"]}
      """

  Scenario: An update can suspend a contact
    Given a customer named "Acme Traders" exists
    When I PUT to /secured/contacts with payload:
      """json
      {"id": "#contact", "status": "SUSPENDED"}
      """
    Then the response status should be 200
    And Contact should exist in database with id #contact and options:
      """json
      {"companyName": "Acme Traders", "status": "SUSPENDED"}
      """

  Scenario: An update cannot leave a contact without a type
    Given a customer named "Acme Traders" exists
    When I PUT to /secured/contacts with payload:
      """json
      {"id": "#contact", "contactTypes": []}
      """
    Then the response status should be 400
    And the response error message should be "At least one contact type required"
    And Contact should exist in database with id #contact and options:
      """json
      {"companyName": "Acme Traders", "contactTypes": ["CUSTOMER"]}
      """

  Scenario: An update cannot turn an organization into a person without a first name
    Given a customer named "Acme Traders" exists
    When I PUT to /secured/contacts with payload:
      """json
      {"id": "#contact", "identityType": "INDIVIDUAL"}
      """
    Then the response status should be 400
    And the response error message should be "Identity type INDIVIDUAL requires first name"
    And Contact should exist in database with id #contact and options:
      """json
      {"companyName": "Acme Traders", "contactTypes": ["CUSTOMER"]}
      """

  Scenario: An update cannot rename a contact to a name another contact already uses
    Given a customer named "Acme Traders" exists
    And a customer named "Beta Ltd" exists
    When I PUT to /secured/contacts with payload:
      """json
      {"id": "#contact", "companyName": "acme traders"}
      """
    Then the response status should be 400
    And the response error message should be "A Contact with company 'acme traders' already exists"
    And Contact should exist in database with id #contact and options:
      """json
      {"companyName": "Beta Ltd"}
      """
    And Contact table should have exactly 3 records in database

  Scenario: An update for a contact that does not exist is rejected
    Given a customer named "Acme Traders" exists
    When I PUT to /secured/contacts with payload:
      """json
      {"id": "00000000-0000-0000-0000-000000000001", "notes": "Ghost"}
      """
    Then the response status should be 400
    And the response error message should be "Attempted to Update non-existent record"
    And Contact table should have exactly 2 records in database
