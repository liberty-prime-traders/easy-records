package app.myliberty.easy_records.locations.business.retail.sale_session.api

import java.util.UUID

data class ConsolidationSourceSale(
    val saleId: UUID,
    val saleVersion: Long,
    val referenceNumber: String,
)
