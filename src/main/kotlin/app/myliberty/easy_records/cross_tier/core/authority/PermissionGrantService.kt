package app.myliberty.easy_records.cross_tier.core.authority

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.core.location_user.api.LocationUserService
import app.myliberty.easy_records.locations.business.core.permission_assignment.api.LocationPermissionAssignmentService
import app.myliberty.easy_records.organizations.business.core.organization_user.api.OrganizationUserService
import app.myliberty.easy_records.organizations.business.core.permission_assignment.api.OrgPermissionAssignmentService
import app.myliberty.easy_records.platform.business.permission_assignment.api.PlatformPermissionAssignmentService
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.stereotype.Service
import java.util.UUID

// Add-on grants are independent of roles and survive a role revocation.
@Service
class PermissionGrantService(
    private val organizationUserService: OrganizationUserService,
    private val locationUserService: LocationUserService,
    private val platformPermissionAssignmentService: PlatformPermissionAssignmentService,
    private val orgPermissionAssignmentService: OrgPermissionAssignmentService,
    private val locationPermissionAssignmentService: LocationPermissionAssignmentService
) {

    fun assignPermission(userId: UUID, permission: EsrPermission) {
        assignPermissions(listOf(userId), listOf(permission))
    }

    fun assignPermissions(userIds: Collection<UUID>, permissions: Collection<EsrPermission>) {
        if (userIds.isEmpty() || permissions.isEmpty()) return
        val assignedById = SessionContextProvider.getUserId()
        permissions.forEach { permission ->
            when (permission.tier) {
                SchemaLevel.PLATFORM -> platformPermissionAssignmentService.assignAll(userIds, permission, assignedById)
                SchemaLevel.ORGANIZATION -> {
                    val orgUserIdsByUserId = organizationUserService.getActiveMembershipIds(userIds)
                    orgPermissionAssignmentService.assignAll(orgUserIdsByUserId, permission, assignedById)
                }
                SchemaLevel.LOCATION -> {
                    val locationUserIdsByUserId = locationUserService.getActiveMembershipIds(userIds)
                    locationPermissionAssignmentService.assignAll(locationUserIdsByUserId, permission, assignedById)
                }
            }
        }
    }

    fun removePermission(userId: UUID, permission: EsrPermission) {
        removePermissions(listOf(userId), listOf(permission))
    }

    fun removePermissions(userIds: Collection<UUID>, permissions: Collection<EsrPermission>) {
        if (userIds.isEmpty() || permissions.isEmpty()) return
        permissions.forEach { permission ->
            when (permission.tier) {
                SchemaLevel.PLATFORM -> platformPermissionAssignmentService.removeAll(userIds, permission)
                SchemaLevel.ORGANIZATION -> orgPermissionAssignmentService.removeAll(userIds, permission)
                SchemaLevel.LOCATION -> locationPermissionAssignmentService.removeAll(userIds, permission)
            }
        }
    }
}
