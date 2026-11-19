package app.myliberty.easy_records.organizations.business.core.organization_user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ExpirableUserAssignmentEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_USER)
@HasReference(tableName = TableName.ORGANIZATION_USER)
class OrganizationUserEntity(

    @Column(name = "join_request_id", updatable = false)
    var joinRequestId: UUID? = null,

) : ExpirableUserAssignmentEntity()
