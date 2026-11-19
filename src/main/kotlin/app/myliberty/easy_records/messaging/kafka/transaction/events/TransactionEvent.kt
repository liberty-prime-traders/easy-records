package app.myliberty.easy_records.messaging.kafka.transaction.events

import app.myliberty.easy_records.messaging.kafka.common.BaseEvent
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import java.util.UUID

sealed class TransactionEvent: BaseEvent() {
    abstract override val sourceContext: EventSourceContext
    abstract val sourceDocumentId: UUID
}
