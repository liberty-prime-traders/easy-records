package app.myliberty.easy_records.locations.business.retail.delivery

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.DeliveryHandlerForPurchase
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryContext
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryHandlerForKafka(
    private val deliveryRepository: PurchaseDeliveryRepository,
    private val deliveryLineRepository: PurchaseDeliveryLineRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase
) : EventReissueHandler {

    override val eventType = PurchaseDeliveredEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val delivery = deliveryRepository.findById(sourceDocumentId)
            .orElseThrow { LibertyGenericException("Delivery $sourceDocumentId not found") }
        val lines = deliveryLineRepository.findByPurchaseDeliveryIdIn(listOf(sourceDocumentId))
            .filter { it.status == PurchaseDeliveryLineStatus.ACTIVE }
        val context = deliveryHandlerForPurchase.getDeliveryContext(delivery.purchaseId)
        publish(context, DeliveryRecord(delivery, lines))
    }

    fun publish(context: PurchaseDeliveryContext, deliveryRecord: DeliveryRecord) {
        val sourceContext = EventSourceContext.LocationLevel(
            orgSchema = SessionContextProvider.getOrganizationSchema(),
            locationSchema = SessionContextProvider.getLocationSchema()
        )
        eventPublisher.publishEvent(
            PurchaseDeliveryMapper.toEvent(
                context.purchaseId,
                context.purchaseReferenceNumber,
                context.supplierId,
                deliveryRecord,
                context.purchaseLineById,
                sourceContext
            )
        )
    }
}
