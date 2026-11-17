package app.myliberty.easy_records.locations.business.core.role_assignment

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
@CacheSchemaLevel(SchemaLevel.LOCATION)
@CacheConfig(cacheNames = [CacheNames.LOCATION_ROLES])
class LocationRoleCache(private val locationRoleAssignmentRepository: LocationRoleAssignmentRepository) {

    @Cacheable
    fun getLocationRoles(userId: UUID): Set<RtsRole> {
        return locationRoleAssignmentRepository.findAllByUserId(userId).map { it.role }.toSet()
    }

    @Cacheable
    fun getGrantedRoles(userId: UUID): List<GrantedRole> {
        return locationRoleAssignmentRepository.findAllByUserId(userId).map { GrantedRole(it.role, it.assignedAt) }
    }

    @CacheEvict(allEntries = true)
    fun insertAll(locationRoleAssignmentEntities: Collection<LocationRoleAssignmentEntity>) {
        locationRoleAssignmentRepository.saveAll(locationRoleAssignmentEntities)
    }

    @CacheEvict(allEntries = true)
    fun remove(userId: UUID, role: RtsRole) {
        locationRoleAssignmentRepository.findByUserIdAndRole(userId, role)
            ?.let { locationRoleAssignmentRepository.delete(it) }
    }

    @CacheEvict(allEntries = true)
    fun removeAllHoldingRole(userIds: Collection<UUID>, role: RtsRole) {
        locationRoleAssignmentRepository.deleteAll(locationRoleAssignmentRepository.findAllByUserIdInAndRole(userIds, role))
    }

    @CacheEvict(allEntries = true)
    fun removeAll(userId: UUID) {
        locationRoleAssignmentRepository.deleteAll(locationRoleAssignmentRepository.findAllByUserId(userId))
    }
}
