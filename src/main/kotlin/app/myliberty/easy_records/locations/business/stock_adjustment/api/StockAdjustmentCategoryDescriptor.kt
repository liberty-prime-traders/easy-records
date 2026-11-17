package app.myliberty.easy_records.locations.business.stock_adjustment.api

import app.myliberty.easy_records.locations.business.stock.api.StockAdjustmentCategory
import java.util.UUID

data class StockAdjustmentCategoryDescriptor(
    val category: StockAdjustmentCategory,
    val displayName: String,
    val allowedDirections: List<StockAdjustmentDirection>,
    val unitCostDirections: List<StockAdjustmentDirection>,
    val requiresReason: Boolean,
    val requiresNote: Boolean,
    val reasons: List<StockAdjustmentReasonOption>
)

data class StockAdjustmentReasonOption(
    val id: UUID,
    val name: String,
    val allowedDirections: List<StockAdjustmentDirection>
)
