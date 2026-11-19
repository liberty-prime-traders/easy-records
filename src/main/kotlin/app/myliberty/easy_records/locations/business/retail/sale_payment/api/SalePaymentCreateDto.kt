package app.myliberty.easy_records.locations.business.retail.sale_payment.api

import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentCreateDto(
    val saleId: UUID? = null,
    val paymentMethodId: UUID,
    val amount: PositiveMoney,
    val reference: String? = null,
    val paymentDate: OffsetDateTime? = null
)
