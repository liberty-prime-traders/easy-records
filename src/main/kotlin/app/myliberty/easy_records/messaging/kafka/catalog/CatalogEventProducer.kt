package app.myliberty.easy_records.messaging.kafka.catalog

import app.myliberty.easy_records.messaging.kafka.common.KafkaConstants
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CatalogEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, CatalogChangedEvent>
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    fun onCatalogChanged(event: CatalogChangedEvent) {
        kafkaTemplate.send(
            KafkaConstants.Topics.CATALOG_EVENTS,
            event.sourceContext.orgSchema,
            event
        )
    }
}
