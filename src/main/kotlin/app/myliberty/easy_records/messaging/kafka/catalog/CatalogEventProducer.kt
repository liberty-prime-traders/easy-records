package app.myliberty.easy_records.messaging.kafka.catalog

import app.myliberty.easy_records.messaging.kafka.common.KafkaConstants
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class CatalogEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, CatalogChangedEvent>
) {

    fun publish(event: CatalogChangedEvent) {
        kafkaTemplate.send(
            KafkaConstants.Topics.CATALOG_EVENTS,
            event.sourceContext.orgSchema,
            event
        )
    }
}
