package app.myliberty.easy_records.locations.business.delivery.api

import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseDeliveryCreateDto(
  val purchaseId: UUID,
  val deliveredAt: OffsetDateTime,
  val notes: String?,
  val lines: List<PurchaseDeliveryLineCreateDto>
)
