package app.myliberty.easy_records.locations.business.retail.delivery.api

import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryStatus
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseDeliveryResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val purchaseId: UUID,
  val status: PurchaseDeliveryStatus,
  val deliveredAt: OffsetDateTime,
  val notes: String?,
  val lines: List<PurchaseDeliveryLineResponseDto>,
  val deliveryTotal: BigDecimal = lines
    .filter { it.status == PurchaseDeliveryLineStatus.ACTIVE }
    .sumOf { it.lineTotal }
)

