package app.myliberty.easy_records.locations.business.purchase.api.bulk_payment

import app.myliberty.easy_records.locations.business.purchase.api.PurchaseStatus
import app.myliberty.easy_records.util.enums.PaymentStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseBulkPaymentResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val purchaseStatus: PurchaseStatus,
  val dateOrdered: OffsetDateTime?,
  val orderedTotal: BigDecimal,
  val paymentCeiling: BigDecimal,
  val paidTotal: BigDecimal,
  val payable: BigDecimal,
  val paymentStatus: PaymentStatus,
  val appliedAmount: BigDecimal,
  val supplierPaymentId: UUID?,
  val paymentReferenceNumber: String?
)
