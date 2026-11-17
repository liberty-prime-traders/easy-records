package app.myliberty.easy_records.locations.business.sale_cogs_backfill

data class SaleLineCogsBackfillCounts(
    val insertedCostRecordCount: Long,
    val saleLinesWithCogsTotalCount: Long,
    val costRecordCount: Long,
)
