package app.myliberty.easy_records.locations.business.stock.api.revision

import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class StockMovementDraft(
    val movementType: MovementType,
    val storageMoved: ExactQuantity,
    val balanceAfterMovement: ExactQuantity,
    val movedCost: BigDecimal,
    val wholeLineQuantityText: String,
    val documentReference: String?,
    val detailReference: String?,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val context: String,
    val stockRevisionReference: String,
)
