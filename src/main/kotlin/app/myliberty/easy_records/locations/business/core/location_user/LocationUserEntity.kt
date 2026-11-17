package app.myliberty.easy_records.locations.business.core.location_user

import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ExpirableUserAssignmentEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames

@Entity
@Table(name = TableNames.LOCATION_USER)
@HasReference(tableName = TableName.LOCATION_USER)
class LocationUserEntity : ExpirableUserAssignmentEntity()
