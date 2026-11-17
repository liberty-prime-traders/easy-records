package app.myliberty.easy_records.organizations.business.kafka_log.api

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.configuration.session.withSession
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.common.KafkaConstants
import app.myliberty.easy_records.messaging.kafka.transaction.events.TransactionEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DltPublisher(
    private val kafkaTemplate: KafkaTemplate<String, TransactionEvent>,
    private val logService: EventProcessingLogService
) {
    private val log = LoggerFactory.getLogger(DltPublisher::class.java)

    fun publish(event: TransactionEvent, consumerGroup: String, logId: UUID) {
        val dltTopic = KafkaConstants.Topics.transactionDlt(consumerGroup)
        // Capture session here; the callback runs on a Kafka producer thread without one.
        val session = SessionContextProvider.getSession().copy()
        val partitionKey = when (val sourceContext = event.sourceContext) {
            is EventSourceContext.LocationLevel -> sourceContext.locationSchema
            is EventSourceContext.OrgLevel -> sourceContext.orgSchema
        }
        kafkaTemplate.send(dltTopic, partitionKey, event)
            .whenComplete { result, throwable ->
                if (throwable == null) {
                    runCatching {
                        withSession(session) {
                            logService.recordDltPublished(
                                logId,
                                result.recordMetadata.partition(),
                                result.recordMetadata.offset()
                            )
                        }
                    }.onFailure {
                        log.error("Failed to record DLT publish result for log $logId, event ${event.eventId}", it)
                    }
                } else {
                    log.error("Failed to publish event ${event.eventId} to DLT $dltTopic", throwable)
                    runCatching {
                        withSession(session) {
                            logService.markDltPublishFailed(
                                logId,
                                throwable.message ?: throwable.javaClass.simpleName
                            )
                        }
                    }.onFailure {
                        log.error("Failed to mark log $logId as DLT_PUBLISH_FAILED for event ${event.eventId}", it)
                    }
                }
            }
    }
}
