package app.myliberty.easy_records.locations.business.sale.api

import java.time.OffsetDateTime

data class SaleVoidInfoDto(
    val voidedBy: String,
    val voidedOn: OffsetDateTime,
    val voidedReason: String?
)
