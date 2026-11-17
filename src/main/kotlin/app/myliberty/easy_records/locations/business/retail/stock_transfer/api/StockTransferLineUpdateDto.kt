package app.myliberty.easy_records.locations.business.stock_transfer.api

import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class StockTransferLineUpdateDto(
    val lineRef: String,
    val quantityDispatched: PositiveUserProvidedQuantity?,
    val unitId: UUID?
)
