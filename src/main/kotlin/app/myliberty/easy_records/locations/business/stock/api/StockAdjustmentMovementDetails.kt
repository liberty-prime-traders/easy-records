package app.myliberty.easy_records.locations.business.stock.api

import java.util.UUID

data class StockAdjustmentMovementDetails(
    val category: StockAdjustmentCategory,
    val reasonId: UUID?,
    val note: String?
)
