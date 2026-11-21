package app.myliberty.easy_records.locations.business.core.role_assignment

import app.myliberty.easy_records.util.enums.EsrRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationRoleAssignmentRepository : JpaRepository<LocationRoleAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<LocationRoleAssignmentEntity>
    fun findByUserIdAndRole(userId: UUID, role: EsrRole): LocationRoleAssignmentEntity?
    fun findAllByUserIdInAndRole(userIds: Collection<UUID>, role: EsrRole): List<LocationRoleAssignmentEntity>
}
