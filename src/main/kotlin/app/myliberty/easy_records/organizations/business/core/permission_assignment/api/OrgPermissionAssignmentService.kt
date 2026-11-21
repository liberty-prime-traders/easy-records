package app.myliberty.easy_records.organizations.business.core.permission_assignment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.cross_tier.core.authority.GrantedPermission
import app.myliberty.easy_records.organizations.business.core.permission_assignment.OrgPermissionAssignmentEntity
import app.myliberty.easy_records.organizations.business.core.permission_assignment.OrgPermissionCache
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.enums.EsrPermission
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrgPermissionAssignmentService(private val orgPermissionCache: OrgPermissionCache) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getPermissions(userId: UUID): Set<EsrPermission> = orgPermissionCache.getOrgPermissions(userId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getGrantedPermissions(userId: UUID): List<GrantedPermission> = orgPermissionCache.getGrantedPermissions(userId)

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun holds(userId: UUID, permission: EsrPermission): Boolean = getPermissions(userId).contains(permission)

    @TransactionalOnOrganizationSchema
    fun assignAll(orgUserIdsByUserId: Map<UUID, UUID>, permission: EsrPermission, assignedById: UUID) {
        val entities = orgUserIdsByUserId.filterKeys { userId -> !holds(userId, permission) }
            .map { (userId, orgUserId) ->
                OrgPermissionAssignmentEntity(
                    userId = userId,
                    permission = permission,
                    assignedAt = DateTimes.Offset.Now.system(),
                    assignedById = assignedById,
                    orgUserId = orgUserId
                )
            }
        if (entities.isNotEmpty()) {
            orgPermissionCache.insertAll(entities)
        }
    }

    @TransactionalOnOrganizationSchema
    fun remove(userId: UUID, permission: EsrPermission) {
        orgPermissionCache.remove(userId, permission)
    }

    @TransactionalOnOrganizationSchema
    fun removeAll(userIds: Collection<UUID>, permission: EsrPermission) {
        orgPermissionCache.removeAllHoldingPermission(userIds, permission)
    }

    @TransactionalOnOrganizationSchema
    fun removeAllPermissions(userId: UUID) {
        orgPermissionCache.removeAll(userId)
    }
}
