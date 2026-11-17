package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ExpenseRecordedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val expenseId: UUID,
    val expenseReferenceNumber: String,
    val expenseAccountCode: String,
    val payeeContactId: UUID,
    val amount: PositiveMoney,
    val expenseDate: LocalDate
) : TransactionEvent() {
    override val sourceDocumentId: UUID = expenseId
}
