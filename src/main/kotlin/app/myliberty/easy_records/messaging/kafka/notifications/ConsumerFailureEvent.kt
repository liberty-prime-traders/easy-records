package app.myliberty.easy_records.messaging.kafka.notifications

import app.myliberty.easy_records.messaging.kafka.common.BaseEvent
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import java.time.Instant
import java.util.UUID

data class ConsumerFailureEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext,
    override val timestamp: Instant,
    override val correlationId: UUID?,
    val failedEventId: UUID,
    val consumerGroup: String,
    val reason: String
) : BaseEvent()
