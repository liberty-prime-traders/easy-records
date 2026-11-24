Feature: Organization users, admins and liberty access
  As an organization administrator
  I want to review who belongs to the organization, what each member can do and who administers it
  So that memberships and admin records can be ended without changing the default organization user

  Scenario: Listing organization users shows each membership by name, email and state
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" has the organization role "MANAGE_ORGANIZATION_USERS"
    And a user "ada@test.local" named "Ada Lovelace" who is an active organization member
    When I GET from secured/organization-users
    Then response returns with status 200
    And response contains details:
      """json
      { "email": "org-user@test.local", "fullName": "Fake Organization User", "active": true }
      """
    And response contains details:
      """json
      { "email": "ada@test.local", "fullName": "Ada Lovelace", "active": true }
      """

  Scenario: The default organization user cannot list organization users
    When I GET from secured/organization-users
    Then response returns with status 403

  Scenario: Access detail lists the roles and add-on permissions granted to a member
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" has the organization role "MANAGE_ORGANIZATION_USERS"
    And a user "ada@test.local" named "Ada Lovelace" who is an active organization member
    And the organization user "ada@test.local" has the organization role "VIEW_ORGANIZATION_USERS"
    And the organization user "ada@test.local" has the add-on permission "MANAGE_ORGANIZATION_ACCESS"
    When I request the access detail of the organization user "ada@test.local"
    Then response returns with status 200
    And response contains details with exact lists:
      """json
      {
        "roles": [ { "role": "VIEW_ORGANIZATION_USERS" } ],
        "addOnPermissions": [ { "permission": "MANAGE_ORGANIZATION_ACCESS" } ]
      }
      """

  Scenario: The default organization user has no roles or add-on permissions
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" has the organization role "MANAGE_ORGANIZATION_USERS"
    When I request the access detail of the organization user "org-user@test.local"
    Then response returns with status 200
    And response contains details with exact lists:
      """json
      { "roles": [], "addOnPermissions": [] }
      """

  Scenario: Terminating a member ends the membership and removes every role and permission
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" has the organization role "MANAGE_ORGANIZATION_USERS"
    And a user "ada@test.local" named "Ada Lovelace" who is an active organization member
    And the organization user "ada@test.local" has the organization role "VIEW_ORGANIZATION_USERS"
    And the organization user "ada@test.local" has the add-on permission "MANAGE_ORGANIZATION_ACCESS"
    When I terminate the organization users "ada@test.local"
    Then response returns with status 200
    And response contains details:
      """json
      { "user": "Ada Lovelace", "endOn": "^.+$" }
      """
    And response list size is 1
    And the organization membership of "ada@test.local" should have ended in database
    And the organization user "ada@test.local" should hold no organization roles or add-on permissions in database

  Scenario: Terminating a member who has already left returns an empty list
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" has the organization role "MANAGE_ORGANIZATION_USERS"
    And a user "ada@test.local" named "Ada Lovelace" who is an active organization member
    When I terminate the organization users "ada@test.local"
    And I terminate the organization users "ada@test.local"
    Then response returns with status 200
    And the response should be an empty list
    And the organization membership of "ada@test.local" should have ended in database

  Scenario: Terminating with no users is rejected and leaves memberships unchanged
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" has the organization role "MANAGE_ORGANIZATION_USERS"
    And a user "ada@test.local" named "Ada Lovelace" who is an active organization member
    When I POST to secured/organization-users/terminate with payload:
      """json
      []
      """
    Then response returns with status 400 with message: No users provided for termination
    And the organization membership of "ada@test.local" should still be active in database

  Scenario: Terminating a member requires the remove-user permission
    Given I am authenticated as a platform admin
    And a user "ada@test.local" named "Ada Lovelace" who is an active organization member
    When I terminate the organization users "ada@test.local"
    Then response returns with status 403
    And the organization membership of "ada@test.local" should still be active in database

  Scenario: Making a user an organization admin records the admin with the user's name
    Given I am authenticated as a platform admin
    And a user "ada@test.local" named "Ada Lovelace" exists
    When I make the organization user "ada@test.local" an organization admin
    Then response returns with status 200
    And response contains details:
      """json
      { "user": "Ada Lovelace", "endOn": null }
      """
    And Organization_Admin table should have exactly 1 records in database

  Scenario: Terminating an organization admin ends the admin record and keeps it
    Given I am authenticated as a platform admin
    And a user "ada@test.local" named "Ada Lovelace" exists
    And the organization user "ada@test.local" is an organization admin
    When I terminate the organization admin "ada@test.local"
    Then response returns with status 204
    And Organization_Admin should match example:
      """json
      { "endOn": "^.+$" }
      """
    And Organization_Admin table should have exactly 1 records in database

  Scenario: Only organization admins can list the admin history
    When I GET from secured/organization-admins
    Then response returns with status 403

  Scenario: The default organization user is not reported as an admin
    When I GET from secured/organization-admins/is-admin
    Then response returns with status 200
    And response contains details:
      """json
      false
      """

  Scenario: A platform admin recorded as an organization admin is reported as an admin
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" is an organization admin
    When I GET from secured/organization-admins/is-admin
    Then response returns with status 200
    And response contains details:
      """json
      true
      """

  Scenario: The default organization user has no liberty permissions
    When I GET from secured/liberty-permissions
    Then response returns with status 200
    And the response should be an empty list

  Scenario: Liberty permissions report only the liberty-level add-on, not permissions bundled in roles
    Given I am authenticated as a platform admin
    And the organization user "platform-admin@test.local" has the organization role "MANAGE_ORGANIZATION_USERS"
    And the organization user "platform-admin@test.local" has the add-on permission "MANAGE_ORGANIZATION_ACCESS"
    When I GET from secured/liberty-permissions
    Then response returns with status 200
    And response contains details with exact lists:
      """json
      [ "MANAGE_ORGANIZATION_ACCESS" ]
      """
