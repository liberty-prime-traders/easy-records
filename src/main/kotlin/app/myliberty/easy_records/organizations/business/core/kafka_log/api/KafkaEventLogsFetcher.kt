package app.myliberty.easy_records.organizations.business.kafka_log.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.kafka_log.EventProcessingLogRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class KafkaEventLogsFetcher(private val eventProcessingLogRepository: EventProcessingLogRepository) {

    fun getEventsForSourceId(sourceId: UUID): List<KafkaEventLogDto> {
        return eventProcessingLogRepository.findBySourceDocumentId(sourceId).map {
            KafkaEventLogDto(
                id = it.id!!,
                sourceDocumentId = it.sourceDocumentId,
                referenceNumber = it.requiredReference(),
                processor = it.consumerGroup,
                status = it.status,
                processedOn = it.processedOn,
                failureReason = it.failureReason
            )
        }
    }
}
