package app.myliberty.easy_records.cucumber.steps.organizations.core

import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.OrganizationAccessFixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.database.SchemaScopedRunner
import app.myliberty.easy_records.organizations.business.core.organization_user.OrganizationUserEntity
import app.myliberty.easy_records.organizations.business.core.organization_user.OrganizationUserRepository
import app.myliberty.easy_records.organizations.business.core.permission_assignment.api.OrgPermissionAssignmentService
import app.myliberty.easy_records.organizations.business.core.role_assignment.api.OrgRoleAssignmentService
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole
import java.util.UUID
import kotlin.test.assertTrue

class OrganizationAccessSteps(
  private val apiClient: ApiClient,
  private val organizationAccessFixtureBuilder: OrganizationAccessFixtureBuilder,
  private val schemaScopedRunner: SchemaScopedRunner,
  private val organizationUserRepository: OrganizationUserRepository,
  private val orgRoleAssignmentService: OrgRoleAssignmentService,
  private val orgPermissionAssignmentService: OrgPermissionAssignmentService,
) {

  @Given("a user {string} named {string} exists")
  fun userExists(userEmail: String, userFullName: String) {
    organizationAccessFixtureBuilder.ensureUser(userEmail, userFullName)
  }

  @Given("a user {string} named {string} who is an active organization member")
  fun userIsActiveMember(userEmail: String, userFullName: String) {
    val userId = organizationAccessFixtureBuilder.ensureUser(userEmail, userFullName)
    organizationAccessFixtureBuilder.ensureActiveMembership(userId)
  }

  @Given("the organization user {string} has the organization role {string}")
  fun userHasOrganizationRole(userEmail: String, roleName: String) {
    val userId = organizationAccessFixtureBuilder.userIdOf(userEmail)
    organizationAccessFixtureBuilder.grantRole(userId, EsrRole.valueOf(roleName))
  }

  @Given("the organization user {string} has the add-on permission {string}")
  fun userHasAddOnPermission(userEmail: String, permissionName: String) {
    val userId = organizationAccessFixtureBuilder.userIdOf(userEmail)
    organizationAccessFixtureBuilder.grantPermission(userId, EsrPermission.valueOf(permissionName))
  }

  @Given("the organization user {string} is an organization admin")
  fun userIsOrganizationAdmin(userEmail: String) {
    makeOrganizationAdmin(userEmail)
  }

  @When("I request the access detail of the organization user {string}")
  fun requestAccessDetail(userEmail: String) {
    val userId = organizationAccessFixtureBuilder.userIdOf(userEmail)
    apiClient.get("/secured/organization-users/access", mapOf("userId" to userId))
  }

  @When("I terminate the organization users {string}")
  fun terminateOrganizationUsers(userEmail: String) {
    val userId = organizationAccessFixtureBuilder.userIdOf(userEmail)
    apiClient.post("/secured/organization-users/terminate", listOf(userId))
  }

  @When("I make the organization user {string} an organization admin")
  fun makeOrganizationAdminWhen(userEmail: String) {
    makeOrganizationAdmin(userEmail)
  }

  @When("I terminate the organization admin {string}")
  fun terminateOrganizationAdmin(userEmail: String) {
    val userId = organizationAccessFixtureBuilder.userIdOf(userEmail)
    apiClient.post("/secured/organization-admins/terminate/$userId")
  }

  @Then("the organization membership of {string} should have ended in database")
  fun membershipShouldHaveEnded(userEmail: String) {
    val memberships = membershipsOf(organizationAccessFixtureBuilder.userIdOf(userEmail))
    assertTrue(
      memberships.isNotEmpty() && memberships.none { membership -> membership.endOn == null },
      "Expected every membership of $userEmail to have ended, found ${memberships.size}",
    )
  }

  @Then("the organization membership of {string} should still be active in database")
  fun membershipShouldStillBeActive(userEmail: String) {
    val memberships = membershipsOf(organizationAccessFixtureBuilder.userIdOf(userEmail))
    assertTrue(
      memberships.any { membership -> membership.endOn == null },
      "Expected $userEmail to hold an active membership",
    )
  }

  @Then("the organization user {string} should hold no organization roles or add-on permissions in database")
  fun userHoldsNoAccess(userEmail: String) {
    val userId = organizationAccessFixtureBuilder.userIdOf(userEmail)
    schemaScopedRunner.run(OrganizationUserEntity::class.java) {
      val grantedRoles = orgRoleAssignmentService.getGrantedRoles(userId)
      val grantedPermissions = orgPermissionAssignmentService.getGrantedPermissions(userId)
      assertTrue(
        grantedRoles.isEmpty() && grantedPermissions.isEmpty(),
        "Expected no grants for $userEmail, found roles $grantedRoles and permissions $grantedPermissions",
      )
    }
  }

  private fun makeOrganizationAdmin(userEmail: String) {
    val userId = organizationAccessFixtureBuilder.userIdOf(userEmail)
    apiClient.post("/secured/organization-admins/$userId")
  }

  private fun membershipsOf(userId: UUID): List<OrganizationUserEntity> =
    schemaScopedRunner.run(OrganizationUserEntity::class.java) {
      organizationUserRepository.findAll().filter { membership -> membership.userId == userId }
    }
}
