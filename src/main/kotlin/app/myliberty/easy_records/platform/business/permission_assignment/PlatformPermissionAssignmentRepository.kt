package app.myliberty.easy_records.platform.business.permission_assignment

import app.myliberty.easy_records.util.enums.EsrPermission
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PlatformPermissionAssignmentRepository : JpaRepository<PlatformPermissionAssignmentEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<PlatformPermissionAssignmentEntity>
    fun findByUserIdAndPermission(userId: UUID, permission: EsrPermission): PlatformPermissionAssignmentEntity?
    fun findAllByPermission(permission: EsrPermission): List<PlatformPermissionAssignmentEntity>
    fun findAllByUserIdInAndPermission(userIds: Collection<UUID>, permission: EsrPermission): List<PlatformPermissionAssignmentEntity>
}
