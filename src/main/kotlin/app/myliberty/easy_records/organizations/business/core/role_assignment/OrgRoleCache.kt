package app.myliberty.easy_records.organizations.business.core.role_assignment

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.configuration.cache.CacheSchemaLevel
import app.myliberty.easy_records.cross_tier.core.authority.GrantedRole
import app.myliberty.easy_records.util.enums.RtsRole
import app.myliberty.easy_records.util.enums.SchemaLevel
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheSchemaLevel(SchemaLevel.ORGANIZATION)
@CacheConfig(cacheNames = [CacheNames.ORG_ROLES])
class OrgRoleCache(private val orgRoleAssignmentRepository: OrgRoleAssignmentRepository) {

    @Cacheable
    fun getOrgRoles(userId: UUID): Set<RtsRole> {
        return orgRoleAssignmentRepository.findAllByUserId(userId).map { it.role }.toSet()
    }

    @Cacheable
    fun getGrantedRoles(userId: UUID): List<GrantedRole> {
        return orgRoleAssignmentRepository.findAllByUserId(userId).map { GrantedRole(it.role, it.assignedAt) }
    }

    @CacheEvict(allEntries = true)
    fun insertAll(orgRoleAssignmentEntities: Collection<OrgRoleAssignmentEntity>) {
        orgRoleAssignmentRepository.saveAll(orgRoleAssignmentEntities)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, role: RtsRole) {
        orgRoleAssignmentRepository.findByUserIdAndRole(userId, role)
            ?.let { orgRoleAssignmentRepository.delete(it) }
    }

    @CacheEvict(allEntries = true)
    fun removeAllHoldingRole(userIds: Collection<UUID>, role: RtsRole) {
        orgRoleAssignmentRepository.deleteAll(orgRoleAssignmentRepository.findAllByUserIdInAndRole(userIds, role))
    }

    @CacheEvict(allEntries = true)
    fun removeAll(userId: UUID) {
        orgRoleAssignmentRepository.deleteAll(orgRoleAssignmentRepository.findAllByUserId(userId))
    }
}
