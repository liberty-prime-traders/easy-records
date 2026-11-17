package app.myliberty.easy_records.locations.business.supplier_payment

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.purchase.api.delivery.DeliveryHandlerForPurchase
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.SupplierPaymentRecordedEvent
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SupplierPaymentHandlerForKafka(
    private val supplierPaymentRepository: SupplierPaymentRepository,
    private val deliveryHandlerForPurchase: DeliveryHandlerForPurchase,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SupplierPaymentRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val payment = supplierPaymentRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Payment $sourceDocumentId not found") }
        val context = deliveryHandlerForPurchase.getDeliveryContext(payment.purchaseId)
        publish(payment, context.supplierId)
    }

    fun publish(payment: SupplierPaymentEntity, supplierId: UUID) {
        eventPublisher.publishEvent(
            SupplierPaymentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                paymentId = payment.id!!,
                supplierId = supplierId,
                paymentMethodAccountCode = payment.paymentMethodAccountCode,
                amount = PositiveMoney.of(payment.amount),
                paymentDate = payment.paymentDate,
                paymentReferenceNumber = payment.requiredReference()
            )
        )
    }
}
