package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class OpeningStockDeclaredEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val openingStockId: UUID,
    val ledgerSourceReferenceNumber: String,
    val locationProductId: UUID,
    val totalCost: Money,
    val postingDate: LocalDate
) : TransactionEvent() {
    override val sourceDocumentId: UUID = openingStockId
}
