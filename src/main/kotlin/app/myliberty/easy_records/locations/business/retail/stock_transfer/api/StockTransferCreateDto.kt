package app.myliberty.easy_records.locations.business.retail.stock_transfer.api

import java.util.UUID

data class StockTransferCreateDto(
    val destinationLocationId: UUID,
    val notes: String?
)
