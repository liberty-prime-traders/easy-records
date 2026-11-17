package app.myliberty.easy_records.configuration.security

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.organization_admin.api.OrganizationAdminService
import app.myliberty.easy_records.organizations.business.organization_user.api.OrganizationUserService
import app.myliberty.easy_records.util.enums.RtsRoleNames
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service("rtsPermissions")
@TransactionalOnPlatformSchema
class RtsPermissions(
    private val organizationAdminService: OrganizationAdminService,
    private val organizationUserService: OrganizationUserService
) {

    fun isPlatformAdmin(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.authorities.any { it.authority == "ROLE_${RtsRoleNames.PLATFORM_ADMIN}" }
    }

    fun isOrganizationAdmin(): Boolean {
        return isPlatformAdmin() || organizationAdminService.isOrganizationAdmin()
    }

    fun isOrganizationMember(): Boolean {
        return organizationUserService.isOrganizationMember(SessionContextProvider.getUserId())
    }

}
