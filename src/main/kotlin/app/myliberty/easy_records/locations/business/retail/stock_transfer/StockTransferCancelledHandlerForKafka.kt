package app.myliberty.easy_records.locations.business.stock_transfer

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockTransferCancelledEvent
import app.myliberty.easy_records.organizations.business.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class StockTransferCancelledHandlerForKafka(
    private val stockTransferDispatchRepository: StockTransferDispatchRepository,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockTransferCancelledEvent::class

    // sourceDocumentId = Transfer Order ID (org schema) — session is source when reissue is called
    @TransactionalOnLocationSchema
    override fun reissue(sourceDocumentId: UUID) {
        val order = stockTransferOrderDataFetcher.getById(sourceDocumentId)
        val dispatch = stockTransferDispatchRepository.findByStockTransferOrderRef(order.referenceNumber)
            ?: throw RtsGenericException("Dispatch not found for order ${order.referenceNumber}")
        publish(
            orderId = order.id,
            orderReferenceNumber = order.referenceNumber,
            dispatchReferenceNumber = dispatch.requiredReference(),
            sourceSchema = SessionContextProvider.getLocationSchema(),
            totalCost = Money.of(order.requiredTotalDispatchedCost())
        )
    }

    fun publish(
        orderId: UUID,
        orderReferenceNumber: String,
        dispatchReferenceNumber: String,
        sourceSchema: String,
        totalCost: Money
    ) {
        eventPublisher.publishEvent(
            StockTransferCancelledEvent(
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
                postingDate = DateTimes.Local.Now.organization()
            )
        )
    }
}
