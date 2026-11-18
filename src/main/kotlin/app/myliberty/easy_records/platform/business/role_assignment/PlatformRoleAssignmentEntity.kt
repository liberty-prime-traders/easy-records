package app.myliberty.easy_records.platform.business.role_assignment

import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.enums.EsrRole
import app.myliberty.easy_records.util.model.RoleAssignmentEntity
import app.myliberty.easy_records.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.PLATFORM_ROLE_ASSIGNMENT)
class PlatformRoleAssignmentEntity(
    userId: UUID,
    role: EsrRole,
    assignedAt: OffsetDateTime,
    assignedById: UUID
) : RoleAssignmentEntity(userId, role, assignedAt, assignedById)
