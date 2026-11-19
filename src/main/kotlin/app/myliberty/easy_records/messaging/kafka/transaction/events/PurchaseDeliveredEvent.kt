package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.Money
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class PurchaseDeliveredEvent(
  override val eventId: UUID,
  override val sourceContext: EventSourceContext.LocationLevel,
  override val timestamp: Instant,
  override val correlationId: UUID?,
  val purchaseId: UUID,
  val purchaseReferenceNumber: String,
  val deliveryId: UUID,
  val deliveryReferenceNumber: String,
  val deliveredAt: OffsetDateTime,
  val supplierId: UUID,
  val lines: List<PurchaseDeliveredLineDto>,
  val deliveryTotal: Money
) : TransactionEvent() {
  override val sourceDocumentId: UUID = deliveryId
}
