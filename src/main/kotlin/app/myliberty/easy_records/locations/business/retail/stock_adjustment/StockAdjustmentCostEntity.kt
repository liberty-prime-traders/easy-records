package app.myliberty.easy_records.locations.business.retail.stock_adjustment

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCostStatus
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCostStatusConverter
import app.myliberty.easy_records.util.annotations.HasReference
import app.myliberty.easy_records.util.model.HasReferenceEntity
import app.myliberty.easy_records.util.model.TableName
import app.myliberty.easy_records.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.STOCK_ADJUSTMENT_COST)
@HasReference(tableName = TableName.STOCK_ADJUSTMENT_COST)
class StockAdjustmentCostEntity(

    @Column(name = "stock_adjustment_id", nullable = false, updatable = false)
    var stockAdjustmentId: UUID,

    @Column(name = "total_cost", nullable = false, precision = 28, scale = 4)
    var totalCost: BigDecimal,

    @Convert(converter = StockAdjustmentCostStatusConverter::class)
    @Column(name = "status", nullable = false, length = 5)
    var status: StockAdjustmentCostStatus = StockAdjustmentCostStatus.ACTIVE

) : HasReferenceEntity()
