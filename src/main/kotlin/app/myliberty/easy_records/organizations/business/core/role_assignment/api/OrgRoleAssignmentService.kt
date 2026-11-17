package app.myliberty.easy_records.organizations.business.core.role_assignment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.cross_tier.core.authority.GrantedRole
import app.myliberty.easy_records.organizations.business.core.role_assignment.OrgRoleAssignmentEntity
import app.myliberty.easy_records.organizations.business.core.role_assignment.OrgRoleCache
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.enums.RtsRole
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrgRoleAssignmentService(private val orgRoleCache: OrgRoleCache) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getRoles(userId: UUID): Set<RtsRole> = orgRoleCache.getOrgRoles(userId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getGrantedRoles(userId: UUID): List<GrantedRole> = orgRoleCache.getGrantedRoles(userId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun holds(userId: UUID, role: RtsRole): Boolean = getRoles(userId).contains(role)

    @TransactionalOnOrganizationSchema
    fun assignAll(orgUserIdsByUserId: Map<UUID, UUID>, role: RtsRole, assignedById: UUID) {
        val entities = orgUserIdsByUserId.filterKeys { userId -> !holds(userId, role) }
            .map { (userId, orgUserId) ->
                OrgRoleAssignmentEntity(
                    userId = userId,
                    role = role,
                    assignedAt = DateTimes.Offset.Now.system(),
                    assignedById = assignedById,
                    orgUserId = orgUserId
                )
            }
        if (entities.isNotEmpty()) {
            orgRoleCache.insertAll(entities)
        }
    }

    @TransactionalOnOrganizationSchema
    fun remove(userId: UUID, role: RtsRole) {
        orgRoleCache.remove(userId, role)
    }

    @TransactionalOnOrganizationSchema
    fun removeAll(userIds: Collection<UUID>, role: RtsRole) {
        orgRoleCache.removeAllHoldingRole(userIds, role)
    }

    @TransactionalOnOrganizationSchema
    fun removeAllRoles(userId: UUID) {
        orgRoleCache.removeAll(userId)
    }
}
