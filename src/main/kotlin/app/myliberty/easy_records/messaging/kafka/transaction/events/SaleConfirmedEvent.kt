package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.Money
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SaleConfirmedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    override val sourceDocumentId: UUID,
    val contactId: UUID,
    val saleReferenceNumber: String,
    val taxableAmount: Money,
    val taxBilled: Money,
    val discountTotal: Money,
    val dateSold: LocalDate,
    val cogsTotal: Money
) : TransactionEvent() {
    val amountOwed: Money = taxableAmount + taxBilled
    val grossRevenue: Money = taxableAmount + discountTotal
}
