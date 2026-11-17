package app.myliberty.easy_records.organizations.business.organization_admin

import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ExpirableUserAssignmentEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.ORGANIZATION_ADMIN)
@HasReference(tableName = TableName.ORGANIZATION_ADMIN)
class OrganizationAdminEntity(userId: UUID): ExpirableUserAssignmentEntity(userId = userId)
