package app.myliberty.easy_records.locations.business.retail.sale.api

import java.util.UUID

data class SaleVoidCreateDto(
    val saleId: UUID,
    val reason: String
)
