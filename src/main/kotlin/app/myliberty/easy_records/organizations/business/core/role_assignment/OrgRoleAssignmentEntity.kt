package app.myliberty.easy_records.organizations.business.core.role_assignment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.enums.RtsRole
import app.myliberty.easy_records.util.model.RoleAssignmentEntity
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.OffsetDateTime
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.ORG_ROLE_ASSIGNMENT)
class OrgRoleAssignmentEntity(
    userId: UUID,
    role: RtsRole,
    assignedAt: OffsetDateTime,
    assignedById: UUID,

    @Column(name = "org_user_id", nullable = false)
    var orgUserId: UUID

) : RoleAssignmentEntity(userId, role, assignedAt, assignedById)
