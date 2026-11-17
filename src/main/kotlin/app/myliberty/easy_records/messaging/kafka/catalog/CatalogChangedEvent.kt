package app.myliberty.easy_records.messaging.kafka.catalog

import app.myliberty.easy_records.messaging.kafka.common.BaseEvent
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.util.model.TableName
import java.util.UUID

data class CatalogChangedEvent(
    override val eventId: UUID,
    override val sourceContext: EventSourceContext.OrgLevel,
    override val timestamp: java.time.Instant,
    override val correlationId: UUID?,
    val tableName: TableName,
    val entityId: UUID
): BaseEvent()
