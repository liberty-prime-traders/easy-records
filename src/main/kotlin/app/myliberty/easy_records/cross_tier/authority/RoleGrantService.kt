package app.myliberty.easy_records.cross_tier.authority

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.location_user.api.LocationUserService
import app.myliberty.easy_records.locations.business.role_assignment.api.LocationRoleAssignmentService
import app.myliberty.easy_records.organizations.business.organization_user.api.OrganizationUserService
import app.myliberty.easy_records.organizations.business.role_assignment.api.OrgRoleAssignmentService
import app.myliberty.easy_records.platform.business.role_assignment.api.PlatformRoleAssignmentService
import app.myliberty.easy_records.util.enums.RtsRole
import app.myliberty.easy_records.util.enums.SchemaLevel
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RoleGrantService(
    private val platformRoleAssignmentService: PlatformRoleAssignmentService,
    private val orgRoleAssignmentService: OrgRoleAssignmentService,
    private val locationRoleAssignmentService: LocationRoleAssignmentService,
    private val organizationUserService: OrganizationUserService,
    private val locationUserService: LocationUserService,
    private val effectiveAuthorizationService: EffectiveAuthorizationService
) {

    fun assignRole(userId: UUID, role: RtsRole) {
        val assignedById = SessionContextProvider.getUserId()
        assignRole(userId, role, assignedById)
    }

    fun assignRoles(userIds: Collection<UUID>, roles: Collection<RtsRole>) {
        if (userIds.isEmpty() || roles.isEmpty()) return
        val assignedById = SessionContextProvider.getUserId()
        roles.forEach { role -> assignRoleToUsers(userIds, role, assignedById) }
    }

    private fun assignRole(userId: UUID, role: RtsRole, assignedById: UUID) {
        assignRoleToUsers(listOf(userId), role, assignedById)
    }

    private fun assignRoleToUsers(userIds: Collection<UUID>, role: RtsRole, assignedById: UUID) {
        requireDependencyRoles(userIds, role)
        when (role.tier) {
            SchemaLevel.PLATFORM -> platformRoleAssignmentService.assignAll(userIds, role, assignedById)
            SchemaLevel.ORGANIZATION -> {
                val orgUserIdsByUserId = organizationUserService.getActiveMembershipIds(userIds)
                orgRoleAssignmentService.assignAll(orgUserIdsByUserId, role, assignedById)
            }
            SchemaLevel.LOCATION -> {
                val locationUserIdsByUserId = locationUserService.getActiveMembershipIds(userIds)
                locationRoleAssignmentService.assignAll(locationUserIdsByUserId, role, assignedById)
            }
        }
    }

    private fun requireDependencyRoles(userIds: Collection<UUID>, role: RtsRole) {
        userIds.forEach { userId ->
            val missingRequiredRoles = role.requiredRoles.filterNot { holds(userId, it) }
            if (missingRequiredRoles.isNotEmpty()) {
                throw RtsGenericException(
                    "Cannot assign role ${role.name} before user gets the following role(s): ${missingRequiredRoles.joinToString { it.name }}"
                )
            }
        }
    }

    private fun holds(userId: UUID, role: RtsRole): Boolean = when (role.tier) {
        SchemaLevel.PLATFORM -> platformRoleAssignmentService.holds(userId, role)
        SchemaLevel.ORGANIZATION -> orgRoleAssignmentService.holds(userId, role)
        SchemaLevel.LOCATION -> locationRoleAssignmentService.holds(userId, role)
    }

    fun removeRole(userId: UUID, role: RtsRole) {
        removeRoleAndDependents(listOf(userId), role)
    }

    fun removeRoles(userIds: Collection<UUID>, roles: Collection<RtsRole>) {
        if (userIds.isEmpty() || roles.isEmpty()) return
        roles.forEach { role -> removeRoleAndDependents(userIds, role) }
    }

    private fun removeRoleAndDependents(userIds: Collection<UUID>, role: RtsRole) {
        when (role.tier) {
            SchemaLevel.PLATFORM -> platformRoleAssignmentService.removeAll(userIds, role)
            SchemaLevel.ORGANIZATION -> orgRoleAssignmentService.removeAll(userIds, role)
            SchemaLevel.LOCATION -> locationRoleAssignmentService.removeAll(userIds, role)
        }
        userIds.forEach { userId ->
            val dependentRoles = effectiveAuthorizationService.getEffectiveRoles(userId).filter { role in it.requiredRoles }
            dependentRoles.forEach { removeRoleAndDependents(listOf(userId), it) }
        }
    }
}
