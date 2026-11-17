package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PurchaseDeliveryRevisedEvent(
  override val eventId: UUID,
  override val sourceContext: EventSourceContext.LocationLevel,
  override val timestamp: Instant,
  override val correlationId: UUID?,
  val stockRevisionId: UUID,
  val stockRevisionReferenceNumber: String,
  val purchaseReferenceNumber: String,
  val supplierId: UUID,
  val revisedLineTotal: Money,
  val replacementLineTotal: Money,
  val postingDate: LocalDate
) : TransactionEvent() {
  override val sourceDocumentId: UUID = stockRevisionId
}
