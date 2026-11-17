package app.myliberty.easy_records.locations.business.retail.sale_cogs_backfill

data class SaleLineCogsBackfillCounts(
    val insertedCostRecordCount: Long,
    val saleLinesWithCogsTotalCount: Long,
    val costRecordCount: Long,
)
