package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentMovementDetails
import app.myliberty.easy_records.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class StockAdjustmentDecreaseStockRequest(
    val locationProductId: UUID,
    val productLabel: String,
    val storageQuantity: ExactQuantity,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val documentReference: String,
    val adjustmentDetails: StockAdjustmentMovementDetails,
)
