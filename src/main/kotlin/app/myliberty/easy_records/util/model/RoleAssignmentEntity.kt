package app.myliberty.easy_records.util.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.MappedSuperclass
import app.myliberty.easy_records.util.enums.RtsRole
import app.myliberty.easy_records.util.enums.RtsRoleConverter
import java.time.OffsetDateTime
import java.util.UUID

@MappedSuperclass
abstract class RoleAssignmentEntity(

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "role", nullable = false, length = 5)
    @Convert(converter = RtsRoleConverter::class)
    var role: RtsRole,

    @Column(name = "assigned_at", nullable = false)
    var assignedAt: OffsetDateTime,

    @Column(name = "assigned_by_id", nullable = false)
    var assignedById: UUID

) : BaseEntity()
