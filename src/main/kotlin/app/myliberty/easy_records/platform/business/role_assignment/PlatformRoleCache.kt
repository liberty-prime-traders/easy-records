package app.myliberty.easy_records.platform.business.role_assignment

import app.myliberty.easy_records.configuration.cache.CacheNames
import app.myliberty.easy_records.cross_tier.core.authority.AuthorityGrant
import app.myliberty.easy_records.util.enums.RtsRole
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@CacheConfig(cacheNames = [CacheNames.PLATFORM_ROLES])
class PlatformRoleCache(private val platformRoleAssignmentRepository: PlatformRoleAssignmentRepository) {

    @Cacheable
    fun getPlatformRoles(userId: UUID): Set<RtsRole> {
        return platformRoleAssignmentRepository.findAllByUserId(userId).map { it.role }.toSet()
    }

    @Cacheable
    fun getHolders(role: RtsRole): List<AuthorityGrant> {
        return platformRoleAssignmentRepository.findAllByRole(role).map { AuthorityGrant(it.userId, it.assignedAt) }
    }

    @CacheEvict(allEntries = true)
    fun insertAll(platformRoleAssignmentEntities: Collection<PlatformRoleAssignmentEntity>) {
        platformRoleAssignmentRepository.saveAll(platformRoleAssignmentEntities)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, role: RtsRole) {
        platformRoleAssignmentRepository.findByUserIdAndRole(userId, role)
            ?.let { platformRoleAssignmentRepository.delete(it) }
    }

    @CacheEvict(allEntries = true)
    fun removeAllHoldingRole(userIds: Collection<UUID>, role: RtsRole) {
        platformRoleAssignmentRepository.deleteAll(platformRoleAssignmentRepository.findAllByUserIdInAndRole(userIds, role))
    }
}
