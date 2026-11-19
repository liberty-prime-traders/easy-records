package app.myliberty.easy_records.organizations.business.core.kafka_log.api

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.DltReader
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.ExpenseRecordedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.TransactionEvent
import app.myliberty.easy_records.organizations.business.core.kafka_log.EventProcessingLogEntity
import app.myliberty.easy_records.organizations.business.core.kafka_log.EventProcessingLogRepository
import app.myliberty.easy_records.organizations.business.core.kafka_log.EventProcessingLogStatus
import app.myliberty.easy_records.organizations.business.core.location.api.LocationDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.kafka.core.KafkaTemplate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class EventRetryServiceTest {

    private val eventProcessingLogRepository = mock(EventProcessingLogRepository::class.java)
    private val locationService = mock(LocationService::class.java)
    private val recordingReissueHandler = RecordingReissueHandler()

    @Suppress("UNCHECKED_CAST")
    private val eventRetryService = EventRetryService(
        eventProcessingLogRepository,
        mock(EventProcessingLogService::class.java),
        locationService,
        mock(KafkaTemplate::class.java) as KafkaTemplate<String, TransactionEvent>,
        mock(DltReader::class.java) as DltReader<TransactionEvent>,
        listOf(recordingReissueHandler)
    )

    private val callerLocationId = UUID.randomUUID()
    private val rowLocationId = UUID.randomUUID()

    @BeforeEach
    fun setCallerSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = callerLocationId, schemaName = "loc-caller")
            )
        )
        `when`(locationService.getById(rowLocationId)).thenReturn(
            LocationDto(rowLocationId, UUID.randomUUID(), OffsetDateTime.now(), "LOC02", schemaName = "loc-row")
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `an org-level row is reissued with no location even when the caller has one`() {
        val logId = stubLogEntry(sourceLocationId = null)

        eventRetryService.retry(logId)

        assertNull(recordingReissueHandler.locationIdSeenDuringReissue)
        assertEquals(callerLocationId, SessionContextProvider.getLocationIdOrNull())
    }

    @Test
    fun `a location row is reissued in its own location rather than the callers`() {
        val logId = stubLogEntry(sourceLocationId = rowLocationId)

        eventRetryService.retry(logId)

        assertEquals(rowLocationId, recordingReissueHandler.locationIdSeenDuringReissue)
        assertEquals(callerLocationId, SessionContextProvider.getLocationIdOrNull())
    }

    @Test
    fun `a caller with no location still has none after a location row is retried`() {
        SessionContextProvider.getSession().location = null
        val logId = stubLogEntry(sourceLocationId = rowLocationId)

        eventRetryService.retry(logId)

        assertEquals(rowLocationId, recordingReissueHandler.locationIdSeenDuringReissue)
        assertNull(SessionContextProvider.getLocationIdOrNull())
    }

    @Test
    fun `the callers location is restored when the reissue fails`() {
        recordingReissueHandler.failure = LibertyGenericException("reissue failed")
        val logId = stubLogEntry(sourceLocationId = rowLocationId)

        assertThrows(LibertyGenericException::class.java) { eventRetryService.retry(logId) }

        assertEquals(callerLocationId, SessionContextProvider.getLocationIdOrNull())
    }

    private fun stubLogEntry(sourceLocationId: UUID?): UUID {
        val logId = UUID.randomUUID()
        val eventProcessingLogEntity = EventProcessingLogEntity(
            eventId = UUID.randomUUID(),
            eventType = ExpenseRecordedEvent::class.simpleName!!,
            consumerGroup = null,
            sourceLocationId = sourceLocationId,
            sourceDocumentId = UUID.randomUUID(),
            status = EventProcessingLogStatus.FAILED
        )
        `when`(eventProcessingLogRepository.findById(logId)).thenReturn(Optional.of(eventProcessingLogEntity))
        return logId
    }

    private class RecordingReissueHandler : EventReissueHandler {
        override val eventType = ExpenseRecordedEvent::class
        var locationIdSeenDuringReissue: UUID? = null
        var failure: RuntimeException? = null

        override fun reissue(sourceDocumentId: UUID) {
            locationIdSeenDuringReissue = SessionContextProvider.getLocationIdOrNull()
            failure?.let { throw it }
        }
    }
}
