package app.myliberty.easy_records.organizations.business.stock_item_source

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.organizations.business.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.stock_item_source.api.StockItemSourceConverter
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames

@Entity
@Table(name = TableNames.STOCK_ITEM_SOURCE)
@HasReference(tableName = TableName.STOCK_ITEM_SOURCE)
class StockItemSourceEntity(

  @Convert(converter = StockItemSourceConverter::class)
  @Column(name = "code", nullable = false, length = 5, unique = true, updatable = false)
  var code: StockItemSource,

  @Column(name = "name", nullable = false, length = 100)
  var name: String,

  @Column(name = "description", columnDefinition = "TEXT")
  var description: String? = null,

  @Column(name = "system_defined", nullable = false, updatable = false)
  var systemDefined: Boolean = false

) : HasReferenceEntity()
