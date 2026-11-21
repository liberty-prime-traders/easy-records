package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class StockTransferDispatchLineStockRequest(
    val dispatchReference: String,
    val dispatchLineReference: String,
    val locationProductId: UUID,
    val storageQuantity: ExactQuantity,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
)
