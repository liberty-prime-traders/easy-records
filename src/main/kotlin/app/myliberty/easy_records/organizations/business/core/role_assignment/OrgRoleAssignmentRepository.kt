package app.myliberty.easy_records.organizations.business.role_assignment

import app.myliberty.easy_records.util.enums.RtsRole
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrgRoleAssignmentRepository : JpaRepository<OrgRoleAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<OrgRoleAssignmentEntity>
    fun findByUserIdAndRole(userId: UUID, role: RtsRole): OrgRoleAssignmentEntity?
    fun findAllByUserIdInAndRole(userIds: Collection<UUID>, role: RtsRole): List<OrgRoleAssignmentEntity>
}
