package app.myliberty.easy_records.locations.business.supplier_payment.api

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SupplierPaymentDto(
    val id: UUID,
    val referenceNumber: String,
    val purchaseId: UUID,
    val deliveryId: UUID?,
    val paymentMethodId: UUID,
    val amount: BigDecimal,
    val paymentDate: OffsetDateTime,
    val notes: String?,
    val createdById: UUID,
    val createdOn: OffsetDateTime
)
