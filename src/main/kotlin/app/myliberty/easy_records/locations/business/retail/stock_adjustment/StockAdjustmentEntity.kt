package app.myliberty.easy_records.locations.business.stock_adjustment

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.locations.business.stock.api.StockAdjustmentCategory
import app.myliberty.easy_records.locations.business.stock.api.StockAdjustmentCategoryConverter
import app.myliberty.easy_records.locations.business.stock_adjustment.api.StockAdjustmentDirection
import app.myliberty.easy_records.locations.business.stock_adjustment.api.StockAdjustmentDirectionConverter
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.ImmutableEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = TableNames.STOCK_ADJUSTMENT)
@HasReference(tableName = TableName.STOCK_ADJUSTMENT)
class StockAdjustmentEntity(

    @Column(name = "location_product_id", nullable = false, updatable = false)
    var locationProductId: UUID,

    @Column(name = "category", nullable = false, updatable = false, length = 5)
    @Convert(converter = StockAdjustmentCategoryConverter::class)
    var category: StockAdjustmentCategory,

    @Column(name = "direction", nullable = false, updatable = false, length = 5)
    @Convert(converter = StockAdjustmentDirectionConverter::class)
    var direction: StockAdjustmentDirection,

    @Column(name = "user_provided_quantity", nullable = false, updatable = false, precision = 19, scale = 4)
    var userProvidedQuantity: BigDecimal,

    @Column(name = "unit_id", nullable = false, updatable = false)
    var unitId: UUID,

    @Column(name = "storage_units_per_user_provided_unit", nullable = false, updatable = false)
    var storageUnitsPerUserProvidedUnit: Long,

    @Column(name = "quantity_text", nullable = false, updatable = false, length = 200)
    var quantityText: String,

    @Column(name = "storage_quantity", nullable = false, updatable = false, precision = 28, scale = 4)
    var storageQuantity: BigDecimal,

    @Column(name = "total_cost", nullable = false, updatable = false, precision = 28, scale = 4)
    var totalCost: BigDecimal,

    @Column(name = "stock_movement_reason_id", updatable = false)
    var stockMovementReasonId: UUID?,

    @Column(name = "note", updatable = false, length = 500)
    var note: String? = null

) : ImmutableEntity()
