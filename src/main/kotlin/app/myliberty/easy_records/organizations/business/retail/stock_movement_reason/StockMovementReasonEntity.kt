package app.myliberty.easy_records.organizations.business.retail.stock_movement_reason

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames

@Entity
@Table(name = TableNames.STOCK_MOVEMENT_REASON)
@HasReference(tableName = TableName.STOCK_MOVEMENT_REASON)
class StockMovementReasonEntity(

  @Column(name = "code", length = 20, updatable = false)
  var code: String? = null,

  @Column(name = "name", nullable = false, length = 100)
  var name: String,

  @Column(name = "description")
  var description: String? = null,

  @Convert(converter = MovementTypeSetConverter::class)
  @Column(name = "movement_types", nullable = false)
  var movementTypes: Set<MovementType>,

  @Column(name = "system_defined", nullable = false, updatable = false)
  var systemDefined: Boolean = false

) : HasReferenceEntity()
