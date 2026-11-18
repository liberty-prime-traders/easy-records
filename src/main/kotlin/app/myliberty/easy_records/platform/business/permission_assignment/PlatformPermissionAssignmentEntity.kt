package app.myliberty.easy_records.platform.business.permission_assignment

import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.enums.EsrPermission
import app.myliberty.easy_records.util.model.PermissionAssignmentEntity
import app.myliberty.easy_records.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.PLATFORM_PERMISSION_ASSIGNMENT)
class PlatformPermissionAssignmentEntity(
    userId: UUID,
    permission: EsrPermission,
    assignedAt: OffsetDateTime,
    assignedById: UUID
) : PermissionAssignmentEntity(userId, permission, assignedAt, assignedById)
