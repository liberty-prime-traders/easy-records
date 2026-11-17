package app.myliberty.easy_records.locations.business.retail.sale_payment.api

import app.myliberty.easy_records.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val saleId: UUID,
    val amount: BigDecimal,
    val reference: String?,
    val paymentDate: OffsetDateTime?,
    val paymentMethodName: String,
    val voidedReason: String? = null,
    val updatedSalePaymentStatus: PaymentStatus? = null,
    val updatedSaleVersion: Long? = null,
)
