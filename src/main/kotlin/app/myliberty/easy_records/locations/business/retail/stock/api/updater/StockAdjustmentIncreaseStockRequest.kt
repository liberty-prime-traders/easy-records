package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentMovementDetails
import app.myliberty.easy_records.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class StockAdjustmentIncreaseStockRequest(
    val locationProductId: UUID,
    val storageQuantity: ExactQuantity,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val totalCost: BigDecimal,
    val documentReference: String,
    val adjustmentDetails: StockAdjustmentMovementDetails,
)
