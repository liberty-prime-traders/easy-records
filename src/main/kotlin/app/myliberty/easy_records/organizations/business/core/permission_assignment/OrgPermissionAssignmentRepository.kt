package app.myliberty.easy_records.organizations.business.core.permission_assignment

import app.myliberty.easy_records.util.enums.EsrPermission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrgPermissionAssignmentRepository : JpaRepository<OrgPermissionAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<OrgPermissionAssignmentEntity>
    fun findByUserIdAndPermission(userId: UUID, permission: EsrPermission): OrgPermissionAssignmentEntity?
    fun findAllByUserIdInAndPermission(userIds: Collection<UUID>, permission: EsrPermission): List<OrgPermissionAssignmentEntity>
}
