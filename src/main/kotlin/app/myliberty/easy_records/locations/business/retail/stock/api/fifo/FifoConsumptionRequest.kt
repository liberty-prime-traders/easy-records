package app.myliberty.easy_records.locations.business.retail.stock.api.fifo

import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentMovementDetails
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class FifoConsumptionRequest(
    val locationProductId: UUID,
    val storageQuantity: ExactQuantity,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val movementType: MovementType,
    val documentReference: String,
    val detailReference: String?,
    val adjustmentDetails: StockAdjustmentMovementDetails? = null,
    val wholeLineQuantityText: String? = null,
    val context: String? = null,
    val stockRevisionReference: String? = null,
)
