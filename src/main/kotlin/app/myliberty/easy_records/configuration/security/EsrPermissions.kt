package app.myliberty.easy_records.configuration.security

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.organization_admin.api.OrganizationAdminService
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserService
import app.myliberty.easy_records.util.enums.EsrRoleNames
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service("esrPermissions")
@TransactionalOnPlatformSchema
class EsrPermissions(
    private val organizationAdminService: OrganizationAdminService,
    private val organizationUserService: OrganizationUserService
) {

    fun isPlatformAdmin(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.authorities.any { it.authority == "ROLE_${EsrRoleNames.PLATFORM_ADMIN}" }
    }

    fun isOrganizationAdmin(): Boolean {
        return isPlatformAdmin() || organizationAdminService.isOrganizationAdmin()
    }

    fun isOrganizationMember(): Boolean {
        return organizationUserService.isOrganizationMember(SessionContextProvider.getUserId())
    }

}
