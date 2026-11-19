package app.myliberty.easy_records.messaging.kafka.transaction

import app.myliberty.easy_records.messaging.kafka.common.BaseEvent
import java.util.UUID
import kotlin.reflect.KClass

interface EventReissueHandler {
    val eventType: KClass<out BaseEvent>
    fun reissue(sourceDocumentId: UUID)
}
