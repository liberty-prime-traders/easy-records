package app.myliberty.easy_records.locations.business.supplier_payment.api

import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.OffsetDateTime
import java.util.UUID

data class SupplierPaymentCreateDto(
    val purchaseId: UUID,
    val deliveryId: UUID?,
    val paymentMethodId: UUID,
    val amount: PositiveMoney,
    val paymentDate: OffsetDateTime,
    val notes: String?
)
