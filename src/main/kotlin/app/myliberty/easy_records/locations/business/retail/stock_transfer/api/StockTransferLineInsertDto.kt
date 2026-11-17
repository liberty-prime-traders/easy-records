package app.myliberty.easy_records.locations.business.stock_transfer.api

import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class StockTransferLineInsertDto(
    val locationProductId: UUID,
    val quantityDispatched: PositiveUserProvidedQuantity
)
