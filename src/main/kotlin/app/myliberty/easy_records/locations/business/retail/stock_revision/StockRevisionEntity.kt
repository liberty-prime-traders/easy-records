package app.myliberty.easy_records.locations.business.stock_revision

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.STOCK_REVISION)
@HasReference(tableName = TableName.STOCK_REVISION)
class StockRevisionEntity(

  @Convert(converter = StockRevisionSourceTypeConverter::class)
  @Column(name = "source_type", nullable = false, updatable = false, length = 5)
  var sourceType: StockRevisionSourceType,

  @Column(name = "source_reference_number", nullable = false, updatable = false, length = 30)
  var sourceReferenceNumber: String,

  @Column(name = "revised_stock_entry_id", nullable = false, updatable = false, unique = true)
  var revisedStockEntryId: UUID,

  @Column(name = "replacement_stock_entry_id", nullable = false, updatable = false, unique = true)
  var replacementStockEntryId: UUID,

  @Column(name = "reason", nullable = false, updatable = false, columnDefinition = "text")
  var reason: String

) : ImmutableEntity()
