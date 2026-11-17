package app.myliberty.easy_records.locations.business.retail.stock_transfer

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.configuration.session.withLocationSchema
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Component
class StockTransferDispatchedHandlerForKafka(
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val locationService: LocationService,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockTransferDispatchedEvent::class

    // sourceDocumentId = order ID (in org schema — accessible regardless of which location context runs reissue)
    override fun reissue(sourceDocumentId: UUID) {
        val order = stockTransferOrderDataFetcher.getById(sourceDocumentId)
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val dispatchReferenceNumber = locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.readDispatchAndLines(order.referenceNumber).dispatch.requiredReference()
        }
        publish(
            orderId = order.id,
            orderReferenceNumber = order.referenceNumber,
            dispatchReferenceNumber = dispatchReferenceNumber,
            sourceSchema = sourceSchema,
            totalCost = Money.of(order.requiredTotalDispatchedCost()),
            postingDate = DateTimes.Local.atOrganizationZone(order.requiredDispatchedAt())
        )
    }

    fun publish(
        orderId: UUID,
        orderReferenceNumber: String,
        dispatchReferenceNumber: String,
        sourceSchema: String,
        totalCost: Money,
        postingDate: LocalDate
    ) {
        eventPublisher.publishEvent(
            StockTransferDispatchedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = sourceSchema
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = orderId,
                orderReferenceNumber = orderReferenceNumber,
                dispatchReferenceNumber = dispatchReferenceNumber,
                totalCost = totalCost,
                postingDate = postingDate
            )
        )
    }
}
