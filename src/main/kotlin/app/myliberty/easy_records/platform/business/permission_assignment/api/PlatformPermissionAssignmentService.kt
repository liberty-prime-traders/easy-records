package app.myliberty.easy_records.platform.business.permission_assignment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.cross_tier.authority.AuthorityGrant
import app.myliberty.easy_records.platform.business.permission_assignment.PlatformPermissionAssignmentEntity
import app.myliberty.easy_records.platform.business.permission_assignment.PlatformPermissionCache
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.enums.RtsPermission
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PlatformPermissionAssignmentService(private val platformPermissionCache: PlatformPermissionCache) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getPermissions(userId: UUID): Set<RtsPermission> = platformPermissionCache.getPlatformPermissions(userId)

    @TransactionalOnPlatformSchema(readOnly = true)
    fun holds(userId: UUID, permission: RtsPermission): Boolean = getPermissions(userId).contains(permission)

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getHolders(permission: RtsPermission): List<AuthorityGrant> = platformPermissionCache.getHolders(permission)

    @TransactionalOnPlatformSchema
    fun assignAll(userIds: Collection<UUID>, permission: RtsPermission, assignedById: UUID) {
        val entities = userIds.filterNot { holds(it, permission) }
            .map { userId ->
                PlatformPermissionAssignmentEntity(
                    userId = userId,
                    permission = permission,
                    assignedAt = DateTimes.Offset.Now.system(),
                    assignedById = assignedById
                )
            }
        if (entities.isNotEmpty()) {
            platformPermissionCache.insertAll(entities)
        }
    }

    @TransactionalOnPlatformSchema
    fun remove(userId: UUID, permission: RtsPermission) {
        platformPermissionCache.remove(userId, permission)
    }

    @TransactionalOnPlatformSchema
    fun removeAll(userIds: Collection<UUID>, permission: RtsPermission) {
        platformPermissionCache.removeAllHoldingPermission(userIds, permission)
    }
}
