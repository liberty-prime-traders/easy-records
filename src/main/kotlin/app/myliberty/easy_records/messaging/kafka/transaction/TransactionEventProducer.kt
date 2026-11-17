package app.myliberty.easy_records.messaging.kafka.transaction

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.configuration.session.withSession
import app.myliberty.easy_records.organizations.business.core.kafka_log.api.EventProcessingLogService
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.common.KafkaConstants
import app.myliberty.easy_records.messaging.kafka.transaction.events.TransactionEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class TransactionEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, TransactionEvent>,
    private val logService: EventProcessingLogService
) {

    private val log = LoggerFactory.getLogger(TransactionEventProducer::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onTransactionEvent(event: TransactionEvent) {
        val session = SessionContextProvider.getSession().copy()
        val partitionKey = when (val sourceContext = event.sourceContext) {
            is EventSourceContext.LocationLevel -> sourceContext.locationSchema
            is EventSourceContext.OrgLevel -> sourceContext.orgSchema
        }
        kafkaTemplate.send(KafkaConstants.Topics.TRANSACTION_EVENTS, partitionKey, event)
            .whenComplete { _, throwable ->
                if (throwable != null) {
                    log.error("Failed to publish event ${event.eventId}", throwable)
                    runCatching {
                        withSession(session) {
                            logService.insertPublishFailed(event, throwable.message ?: throwable.javaClass.simpleName)
                        }
                    }.onFailure {
                        log.error("Failed to record publish failure for event ${event.eventId}", it)
                    }
                }
            }
    }
}
