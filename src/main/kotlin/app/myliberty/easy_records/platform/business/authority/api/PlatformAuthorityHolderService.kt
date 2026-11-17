package app.myliberty.easy_records.platform.business.authority.api

import app.myliberty.easy_records.cross_tier.core.authority.AuthorityHolderResponse
import app.myliberty.easy_records.cross_tier.core.authority.AuthorityType
import app.myliberty.easy_records.platform.business.authority.PlatformAuthorityLookup
import app.myliberty.easy_records.platform.business.permission_assignment.api.PlatformPermissionAssignmentService
import app.myliberty.easy_records.platform.business.role_assignment.api.PlatformRoleAssignmentService
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service

@Service
class PlatformAuthorityHolderService(
    private val platformRoleAssignmentService: PlatformRoleAssignmentService,
    private val platformPermissionAssignmentService: PlatformPermissionAssignmentService,
    private val userQualifier: UserQualifier
) {

    fun getHolders(authorityName: String, authorityType: AuthorityType): List<AuthorityHolderResponse> {
        val lookup = PlatformAuthorityLookup.of(authorityName, authorityType)
        val (grants, resolvedName) = when (lookup) {
            is PlatformAuthorityLookup.ByRole ->
                platformRoleAssignmentService.getHolders(lookup.role) to lookup.role.name
            is PlatformAuthorityLookup.ByPermission ->
                platformPermissionAssignmentService.getHolders(lookup.permission) to lookup.permission.name
        }
        return grants.map {
            AuthorityHolderResponse(
                fullName = userQualifier.getUserFullName(it.userId),
                grantedOn = it.grantedAt,
                authorityName = resolvedName
            )
        }
    }
}
