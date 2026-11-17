package app.myliberty.easy_records.platform.business.role_assignment

import app.myliberty.easy_records.util.enums.RtsRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlatformRoleAssignmentRepository : JpaRepository<PlatformRoleAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<PlatformRoleAssignmentEntity>
    fun findByUserIdAndRole(userId: UUID, role: RtsRole): PlatformRoleAssignmentEntity?
    fun findAllByRole(role: RtsRole): List<PlatformRoleAssignmentEntity>
    fun findAllByUserIdInAndRole(userIds: Collection<UUID>, role: RtsRole): List<PlatformRoleAssignmentEntity>
}
