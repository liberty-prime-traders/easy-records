package app.myliberty.easy_records.cucumber.support.initialization

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.organization_user.OrganizationUserCache
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrganizationMembershipSeeder(private val organizationUserCache: OrganizationUserCache) {

  // Needs the organization session. Membership only, no roles: scenarios that test access grant their own.
  @TransactionalOnOrganizationSchema
  fun ensureMember(userId: UUID) {
    if (organizationUserCache.getOrganizationUsers().any { it.userId == userId }) return
    organizationUserCache.create(OrganizationUserInsertDto(userId = userId))
  }
}
