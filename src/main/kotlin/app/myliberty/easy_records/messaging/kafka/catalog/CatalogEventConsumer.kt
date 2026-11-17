package app.myliberty.easy_records.messaging.kafka.catalog

import app.myliberty.easy_records.messaging.kafka.common.KafkaConstants
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class CatalogEventConsumer(
    private val catalogEventHandler: CatalogEventHandler
) {

    @KafkaListener(
        topics = [KafkaConstants.Topics.CATALOG_EVENTS],
        groupId = KafkaConstants.ConsumerGroups.Catalog.SYNC
    )
    fun onCatalogEvent(event: CatalogChangedEvent) {
        catalogEventHandler.consume(event)
    }
}
