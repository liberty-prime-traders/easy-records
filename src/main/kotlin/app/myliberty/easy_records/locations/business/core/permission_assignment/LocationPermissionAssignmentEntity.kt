package app.myliberty.easy_records.locations.business.core.permission_assignment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.enums.RtsPermission
import app.myliberty.easy_records.util.model.PermissionAssignmentEntity
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.LOCATION_PERMISSION_ASSIGNMENT)
class LocationPermissionAssignmentEntity(
    userId: UUID,
    permission: RtsPermission,
    assignedAt: OffsetDateTime,
    assignedById: UUID,

    @Column(name = "location_user_id", nullable = false)
    var locationUserId: UUID

) : PermissionAssignmentEntity(userId, permission, assignedAt, assignedById)
