package app.myliberty.easy_records.platform.business.role_assignment.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnPlatformSchema
import app.myliberty.easy_records.cross_tier.core.authority.AuthorityGrant
import app.myliberty.easy_records.platform.business.role_assignment.PlatformRoleAssignmentEntity
import app.myliberty.easy_records.platform.business.role_assignment.PlatformRoleCache
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.enums.EsrRole
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PlatformRoleAssignmentService(private val platformRoleCache: PlatformRoleCache) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getRoles(userId: UUID): Set<EsrRole> = platformRoleCache.getPlatformRoles(userId)

    @TransactionalOnPlatformSchema(readOnly = true)
    fun holds(userId: UUID, role: EsrRole): Boolean = getRoles(userId).contains(role)

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getHolders(role: EsrRole): List<AuthorityGrant> = platformRoleCache.getHolders(role)

    @TransactionalOnPlatformSchema
    fun assignAll(userIds: Collection<UUID>, role: EsrRole, assignedById: UUID) {
        val entities = userIds.filterNot { holds(it, role) }
            .map { userId ->
                PlatformRoleAssignmentEntity(
                    userId = userId,
                    role = role,
                    assignedAt = DateTimes.Offset.Now.system(),
                    assignedById = assignedById
                )
            }
        if (entities.isNotEmpty()) {
            platformRoleCache.insertAll(entities)
        }
    }

    @TransactionalOnPlatformSchema
    fun remove(userId: UUID, role: EsrRole) {
        platformRoleCache.remove(userId, role)
    }

    @TransactionalOnPlatformSchema
    fun removeAll(userIds: Collection<UUID>, role: EsrRole) {
        platformRoleCache.removeAllHoldingRole(userIds, role)
    }
}
