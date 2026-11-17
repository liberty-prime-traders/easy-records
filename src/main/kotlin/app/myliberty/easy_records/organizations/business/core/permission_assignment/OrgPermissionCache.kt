package app.myliberty.easy_records.organizations.business.permission_assignment

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.cross_tier.authority.GrantedPermission
import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORG_PERMISSIONS])
class OrgPermissionCache(private val orgPermissionAssignmentRepository: OrgPermissionAssignmentRepository) {

    @Cacheable
    fun getOrgPermissions(userId: UUID): Set<RtsPermission> {
        return orgPermissionAssignmentRepository.findAllByUserId(userId).map { it.permission }.toSet()
    }

    @Cacheable
    fun getGrantedPermissions(userId: UUID): List<GrantedPermission> {
        return orgPermissionAssignmentRepository.findAllByUserId(userId).map { GrantedPermission(it.permission, it.assignedAt) }
    }

    @CacheEvict(allEntries = true)
    fun insertAll(orgPermissionAssignmentEntities: Collection<OrgPermissionAssignmentEntity>) {
        orgPermissionAssignmentRepository.saveAll(orgPermissionAssignmentEntities)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, permission: RtsPermission) {
        orgPermissionAssignmentRepository.findByUserIdAndPermission(userId, permission)
            ?.let { orgPermissionAssignmentRepository.delete(it) }
    }

    @CacheEvict(allEntries = true)
    fun removeAllHoldingPermission(userIds: Collection<UUID>, permission: RtsPermission) {
        orgPermissionAssignmentRepository.deleteAll(
            orgPermissionAssignmentRepository.findAllByUserIdInAndPermission(userIds, permission)
        )
    }

    @CacheEvict(allEntries = true)
    fun removeAll(userId: UUID) {
        orgPermissionAssignmentRepository.deleteAll(orgPermissionAssignmentRepository.findAllByUserId(userId))
    }
}
