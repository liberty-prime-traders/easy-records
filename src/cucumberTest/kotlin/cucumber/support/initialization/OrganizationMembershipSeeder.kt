package app.myliberty.easy_records.cucumber.support.initialization

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.organization_user.OrganizationUserCache
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationMembershipSeeder(private val organizationUserCache: OrganizationUserCache) {

  // Needs the organization session. Membership only, no roles: scenarios that test access grant their own.
  // The membership table survives the reset, so a membership that a scenario ended is reactivated here.
  @TransactionalOnOrganizationSchema
  fun ensureMember(userId: UUID) {
    val existingMembership = organizationUserCache.getOrganizationUsers().find { it.userId == userId }
    when {
      existingMembership == null -> organizationUserCache.create(OrganizationUserInsertDto(userId = userId))
      !existingMembership.isActive() -> organizationUserCache.save(existingMembership.copy(endOn = null))
    }
  }
}
