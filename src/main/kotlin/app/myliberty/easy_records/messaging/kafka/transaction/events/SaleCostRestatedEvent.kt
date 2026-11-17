package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// Totals of every sale line a stock revision restated, so one posting carries the whole revision.
data class SaleCostRestatedEvent(
  override val eventId: UUID,
  override val sourceContext: EventSourceContext.LocationLevel,
  override val timestamp: Instant,
  override val correlationId: UUID?,
  val stockRevisionId: UUID,
  val stockRevisionReferenceNumber: String,
  val oldCostTotal: Money,
  val newCostTotal: Money,
  val postingDate: LocalDate
) : TransactionEvent() {
  override val sourceDocumentId: UUID = stockRevisionId
}
