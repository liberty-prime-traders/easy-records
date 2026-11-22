package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentCategory
import java.time.OffsetDateTime
import java.util.UUID

data class StockAdjustmentSearchParameters(
    val adjustedFrom: OffsetDateTime? = null,
    val adjustedBefore: OffsetDateTime? = null,
    val locationProductIds: List<UUID> = emptyList(),
    val categories: List<StockAdjustmentCategory> = emptyList(),
    val adjustedByIds: List<UUID> = emptyList()
)
