package app.myliberty.easy_records.locations.business.retail.stock_transfer

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.configuration.session.withLocationSchema
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockTransferReceiptCompletedEvent
import app.myliberty.easy_records.organizations.business.core.location.api.LocationService
import app.myliberty.easy_records.organizations.business.retail.stock_transfer.api.StockTransferOrderDataFetcher
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class StockTransferReceiptCompletedHandlerForKafka(
    private val stockTransferReceiptRepository: StockTransferReceiptRepository,
    private val stockTransferSchemaGateway: StockTransferSchemaGateway,
    private val stockTransferOrderDataFetcher: StockTransferOrderDataFetcher,
    private val locationService: LocationService,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockTransferReceiptCompletedEvent::class

    // sourceDocumentId = receipt ID (destination schema) — session is the destination when reissue is called
    @TransactionalOnLocationSchema(readOnly = true)
    override fun reissue(sourceDocumentId: UUID) {
        val receipt = stockTransferReceiptRepository.findById(sourceDocumentId)
            .orElseThrow { LibertyGenericException("Stock transfer receipt $sourceDocumentId not found") }
        val order = stockTransferOrderDataFetcher.getByReferenceNumber(receipt.stockTransferOrderRef)
        val sourceSchema = locationService.getSchemaByLocationId(order.sourceLocationId)
        val dispatchReferenceNumber = locationService.withLocationSchema(sourceSchema) {
            stockTransferSchemaGateway.readDispatchAndLines(order.referenceNumber).dispatch.requiredReference()
        }
        publish(
            receiptId = receipt.id!!,
            orderReferenceNumber = receipt.stockTransferOrderRef,
            dispatchReferenceNumber = dispatchReferenceNumber,
            receiptReferenceNumber = receipt.requiredReference(),
            totalCost = Money.of(order.requiredTotalDispatchedCost())
        )
    }

    fun publish(
        receiptId: UUID,
        orderReferenceNumber: String,
        dispatchReferenceNumber: String,
        receiptReferenceNumber: String,
        totalCost: Money
    ) {
        eventPublisher.publishEvent(
            StockTransferReceiptCompletedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = receiptId,
                orderReferenceNumber = orderReferenceNumber,
                dispatchReferenceNumber = dispatchReferenceNumber,
                totalCost = totalCost,
                receiptReferenceNumber = receiptReferenceNumber,
                postingDate = DateTimes.Local.Now.organization()
            )
        )
    }
}
