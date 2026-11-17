package app.myliberty.easy_records.util.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.MappedSuperclass
import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.enums.RtsPermissionConverter
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class PermissionAssignmentEntity(

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "permission", nullable = false, length = 5)
    @Convert(converter = RtsPermissionConverter::class)
    var permission: RtsPermission,

    @Column(name = "assigned_at", nullable = false)
    var assignedAt: OffsetDateTime,

    @Column(name = "assigned_by_id", nullable = false)
    var assignedById: UUID

) : BaseEntity()
