package app.myliberty.easy_records.locations.business.stock_adjustment_backfill

import java.util.UUID

data class StockAdjustmentBackfillResult(
    val locationId: UUID,
    val locationName: String,
    val counts: StockAdjustmentBackfillCounts,
) {
    val isReconciled: Boolean = counts.adjustmentCount == counts.activeCostRecordCount
}
