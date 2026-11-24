package app.myliberty.easy_records.cucumber.fixtures.organizations.core

import app.myliberty.easy_records.configuration.session.ServiceAccountContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.PersistentKey
import app.myliberty.easy_records.organizations.business.core.organization_user.OrganizationUserCache
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserInsertDto
import app.myliberty.easy_records.organizations.business.core.permission_assignment.api.OrgPermissionAssignmentService
import app.myliberty.easy_records.organizations.business.core.role_assignment.api.OrgRoleAssignmentService
import app.myliberty.easy_records.platform.business.organization.OrganizationCache
import app.myliberty.easy_records.platform.business.organization.api.OrganizationDto
import app.myliberty.easy_records.platform.business.sysuser.api.SysUserService
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.EsrRole
import app.myliberty.easy_records.util.enums.ServiceAccount
import org.springframework.stereotype.Component
import java.util.UUID

// Grants go through the services inside the organization session, because the platform admin cannot grant org
// access through the API after a reset. The same route creates the boilerplate membership.
@Component
class OrganizationAccessFixtureBuilder(
  private val injectContext: InjectContext,
  private val organizationCache: OrganizationCache,
  private val organizationUserCache: OrganizationUserCache,
  private val orgRoleAssignmentService: OrgRoleAssignmentService,
  private val orgPermissionAssignmentService: OrgPermissionAssignmentService,
  private val sysUserService: SysUserService,
) {

  fun ensureUser(email: String, fullName: String): UUID {
    val existingUser = sysUserService.findByEmail(email)
    if (existingUser != null) return existingUser.id
    val fullNameParts = fullName.split(" ", limit = 2)
    return sysUserService.createUser(
      email = email,
      localFirstName = fullNameParts.first(),
      localLastName = fullNameParts.getOrNull(1),
    )
  }

  fun userIdOf(email: String): UUID =
    checkNotNull(sysUserService.findByEmail(email)?.id) { "No user with email $email" }

  fun ensureActiveMembership(userId: UUID) {
    inOrganizationSession {
      val hasActiveMembership = organizationUserCache.getOrganizationUsers()
        .any { membership -> membership.userId == userId && membership.isActive() }
      if (!hasActiveMembership) {
        organizationUserCache.create(OrganizationUserInsertDto(userId = userId))
      }
    }
  }

  fun grantRole(userId: UUID, role: EsrRole) {
    inOrganizationSession {
      val membershipIdsByUserId = mapOf(userId to activeMembershipIdOf(userId))
      orgRoleAssignmentService.assignAll(membershipIdsByUserId, role, userIdOf(PLATFORM_ADMIN_EMAIL))
    }
  }

  fun grantPermission(userId: UUID, permission: EsrPermission) {
    inOrganizationSession {
      val membershipIdsByUserId = mapOf(userId to activeMembershipIdOf(userId))
      orgPermissionAssignmentService.assignAll(membershipIdsByUserId, permission, userIdOf(PLATFORM_ADMIN_EMAIL))
    }
  }

  private fun activeMembershipIdOf(userId: UUID): UUID =
    checkNotNull(
      organizationUserCache.getOrganizationUsers()
        .find { membership -> membership.userId == userId && membership.isActive() }?.id
    ) { "User $userId has no active organization membership" }

  private fun <T> inOrganizationSession(block: () -> T): T {
    val organizationId = injectContext.get(PersistentKey.ORGANIZATION)
    val organization: OrganizationDto = checkNotNull(
      organizationCache.getAllOrganizations().find { organization -> organization.id == organizationId }
    ) { "Organization $organizationId not found" }
    return ServiceAccountContext.runWithServiceAccount<T>(ServiceAccount.RECORD_INITIALIZER) {
      SessionContextProvider.initOrganization(organization)
      try {
        block()
      } finally {
        SessionContextProvider.clear()
      }
    }
  }

  companion object {
    const val PLATFORM_ADMIN_EMAIL = "platform-admin@test.local"
  }
}
