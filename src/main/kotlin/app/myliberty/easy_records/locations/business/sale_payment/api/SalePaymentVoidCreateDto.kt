package app.myliberty.easy_records.locations.business.sale_payment.api

import java.util.UUID

data class SalePaymentVoidCreateDto(
    val salePaymentId: UUID,
    val reason: String
)
