package app.myliberty.easy_records.messaging.kafka.common

import java.time.Instant
import java.util.UUID

abstract class BaseEvent {
    abstract val eventId: UUID
    abstract val sourceContext: EventSourceContext
    abstract val timestamp: Instant
    abstract val correlationId: UUID?
}
