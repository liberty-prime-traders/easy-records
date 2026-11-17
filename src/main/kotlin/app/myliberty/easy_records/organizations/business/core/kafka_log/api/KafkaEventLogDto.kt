package app.myliberty.easy_records.organizations.business.kafka_log.api

import app.myliberty.easy_records.organizations.business.kafka_log.EventProcessingLogStatus
import java.time.Instant
import java.util.UUID

data class KafkaEventLogDto(
    val id: UUID,
    val sourceDocumentId: UUID,
    val referenceNumber: String,
    val processor: String?,
    val status: EventProcessingLogStatus,
    val processedOn: Instant? = null,
    val failureReason: String? = null,
)
