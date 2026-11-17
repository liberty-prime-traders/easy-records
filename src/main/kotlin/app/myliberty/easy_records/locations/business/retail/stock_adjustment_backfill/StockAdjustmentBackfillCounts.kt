package app.myliberty.easy_records.locations.business.retail.stock_adjustment_backfill

data class StockAdjustmentBackfillCounts(
    val insertedCostRecordCount: Long,
    val adjustmentCount: Long,
    val activeCostRecordCount: Long,
)
