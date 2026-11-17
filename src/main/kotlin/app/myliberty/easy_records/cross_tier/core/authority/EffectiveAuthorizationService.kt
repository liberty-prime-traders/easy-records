package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.core.permission_assignment.api.LocationPermissionAssignmentService
import app.myliberty.easy_records.locations.business.core.role_assignment.api.LocationRoleAssignmentService
import app.myliberty.easy_records.organizations.business.core.permission_assignment.api.OrgPermissionAssignmentService
import app.myliberty.easy_records.organizations.business.core.role_assignment.api.OrgRoleAssignmentService
import app.myliberty.easy_records.platform.business.permission_assignment.api.PlatformPermissionAssignmentService
import app.myliberty.easy_records.platform.business.role_assignment.api.PlatformRoleAssignmentService
import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.RtsRole
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EffectiveAuthorizationService(
    private val platformRoleAssignmentService: PlatformRoleAssignmentService,
    private val orgRoleAssignmentService: OrgRoleAssignmentService,
    private val locationRoleAssignmentService: LocationRoleAssignmentService,
    private val platformPermissionAssignmentService: PlatformPermissionAssignmentService,
    private val orgPermissionAssignmentService: OrgPermissionAssignmentService,
    private val locationPermissionAssignmentService: LocationPermissionAssignmentService
) {

    fun getEffectiveRoles(userId: UUID): Set<RtsRole> {
        val roles = mutableSetOf<RtsRole>()
        roles += platformRoleAssignmentService.getRoles(userId)
        if (SessionContextProvider.getOrganizationIdOrNull() != null) {
            roles += orgRoleAssignmentService.getRoles(userId)
        }
        if (SessionContextProvider.getLocationIdOrNull() != null) {
            roles += locationRoleAssignmentService.getRoles(userId)
        }
        return roles
    }

    fun getEffectivePermissions(userId: UUID): Set<RtsPermission> {
        val fromRoles = getEffectiveRoles(userId).flatMap { it.permissions }
        val directPlatform = platformPermissionAssignmentService.getPermissions(userId)
        val directOrg = if (SessionContextProvider.getOrganizationIdOrNull() != null) {
            orgPermissionAssignmentService.getPermissions(userId)
        } else {
            emptySet()
        }
        val directLocation = if (SessionContextProvider.getLocationIdOrNull() != null) {
            locationPermissionAssignmentService.getPermissions(userId)
        } else {
            emptySet()
        }
        return (fromRoles + directPlatform + directOrg + directLocation).toSet()
    }
}
