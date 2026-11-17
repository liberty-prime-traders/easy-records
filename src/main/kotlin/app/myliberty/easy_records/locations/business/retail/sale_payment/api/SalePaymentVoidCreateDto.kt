package app.myliberty.easy_records.locations.business.retail.sale_payment.api

import java.util.UUID

data class SalePaymentVoidCreateDto(
    val salePaymentId: UUID,
    val reason: String
)
