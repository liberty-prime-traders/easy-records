package app.myliberty.easy_records.locations.business.purchase.api

import app.myliberty.easy_records.util.enums.PaymentStatus
import app.myliberty.easy_records.locations.business.delivery.api.PurchaseDeliveryResponseDto
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val supplierId: UUID,
  val supplierName: String?,
  val purchaseStatus: PurchaseStatus,
  val paymentStatus: PaymentStatus,
  val notes: String?,
  val dateOrdered: OffsetDateTime?,
  val orderedBy: String?,
  val orderedById: UUID?,
  val createdBy: String?,
  val createdOn: OffsetDateTime?,
  val lines: List<PurchaseLineResponseDto>,
  val orderedTotal: BigDecimal,
  val deliveredTotal: BigDecimal,
  val paymentCeiling: BigDecimal,
  val deliveries: List<PurchaseDeliveryResponseDto>
)
