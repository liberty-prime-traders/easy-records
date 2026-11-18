package app.myliberty.easy_records.locations.business.retail.opening_stock

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.OpeningStockDeclaredEvent
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class OpeningStockHandlerForKafka(
    private val openingStockRepository: OpeningStockRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = OpeningStockDeclaredEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val row = openingStockRepository.findById(sourceDocumentId)
            .orElseThrow { LibertyGenericException("Opening stock $sourceDocumentId not found") }
        publish(row)
    }

    fun publish(openingStock: OpeningStockEntity) {
        eventPublisher.publishEvent(
            OpeningStockDeclaredEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                openingStockId = requireNotNull(openingStock.id),
                ledgerSourceReferenceNumber = openingStock.requiredReference(),
                locationProductId = openingStock.locationProductId,
                totalCost = Money.of(openingStock.totalCost),
                postingDate = DateTimes.Local.atOrganizationZone(openingStock.requiredCreatedOn())
            )
        )
    }
}
