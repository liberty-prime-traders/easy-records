package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

data class SalePaymentRecordedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    override val sourceDocumentId: UUID,
    val contactId: UUID,
    val payments: List<SalePaymentLineDto>
) : TransactionEvent()

data class SalePaymentLineDto(
    val paymentReferenceNumber: String,
    val paymentMethodAccountCode: String,
    val amount: PositiveMoney,
    val paymentDate: OffsetDateTime
)
