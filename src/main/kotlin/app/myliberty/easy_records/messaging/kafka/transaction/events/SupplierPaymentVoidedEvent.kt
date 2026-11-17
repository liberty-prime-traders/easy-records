package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.business.PositiveMoney
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SupplierPaymentVoidedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.LocationLevel,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val voidId: UUID,
    val paymentId: UUID,
    val supplierId: UUID,
    val paymentMethodAccountCode: String,
    val amount: PositiveMoney,
    val voidedOn: LocalDate,
    val paymentReferenceNumber: String
) : TransactionEvent() {
    override val sourceDocumentId: UUID = voidId
}
