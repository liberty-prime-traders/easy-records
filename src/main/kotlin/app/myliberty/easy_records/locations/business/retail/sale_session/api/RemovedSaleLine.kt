package app.myliberty.easy_records.locations.business.retail.sale_session.api

data class RemovedSaleLine(
    val saleLine: SaleSessionLine,
    val saleAdjustments: List<SaleSessionAdjustment>,
)
