package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class ExpensePaymentRecordedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val paymentId: UUID,
    val paymentReferenceNumber: String,
    val expenseAccountCode: String,
    val payeeContactId: UUID,
    val paymentMethodAccountCode: String,
    val amount: PositiveMoney,
    val paymentDate: OffsetDateTime
) : TransactionEvent() {
    override val sourceDocumentId: UUID = paymentId
}
