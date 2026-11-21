package app.myliberty.easy_records.platform.business.role_assignment

import app.myliberty.easy_records.util.enums.EsrRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlatformRoleAssignmentRepository : JpaRepository<PlatformRoleAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<PlatformRoleAssignmentEntity>
    fun findByUserIdAndRole(userId: UUID, role: EsrRole): PlatformRoleAssignmentEntity?
    fun findAllByRole(role: EsrRole): List<PlatformRoleAssignmentEntity>
    fun findAllByUserIdInAndRole(userIds: Collection<UUID>, role: EsrRole): List<PlatformRoleAssignmentEntity>
}
