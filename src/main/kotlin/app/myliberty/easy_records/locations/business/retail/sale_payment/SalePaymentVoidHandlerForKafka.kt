package app.myliberty.easy_records.locations.business.retail.sale_payment

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.SalePaymentVoidedEvent
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.PositiveMoney
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentVoidHandlerForKafka(
    private val saleDataFetcher: SaleDataFetcher,
    private val salePaymentRepository: SalePaymentRepository,
    private val salePaymentVoidRepository: SalePaymentVoidRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SalePaymentVoidedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val payments = salePaymentRepository.findBySaleId(sourceDocumentId)
        if (payments.isEmpty()) return
        val voidsByPaymentId = salePaymentVoidRepository.findBySalePaymentIdIn(payments.map { it.id!! })
            .associateBy { it.salePaymentId }
        val voidedSalePayments = payments.mapNotNull { payment ->
            voidsByPaymentId[payment.id!!]?.let { VoidedSalePayment(payment, it) }
        }
        publish(saleDataFetcher.getSaleContactId(sourceDocumentId), voidedSalePayments)
    }

    fun publish(contactId: UUID, voidedSalePayments: List<VoidedSalePayment>) {
        voidedSalePayments.forEach { publishEvent(contactId, it) }
    }

    private fun publishEvent(contactId: UUID, voidedSalePayment: VoidedSalePayment) {
        val payment = voidedSalePayment.payment
        eventPublisher.publishEvent(
            SalePaymentVoidedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = payment.saleId,
                contactId = contactId,
                paymentReferenceNumber = payment.requiredReference(),
                paymentMethodAccountCode = payment.paymentMethodAccountCode,
                amount = PositiveMoney.of(payment.amount),
                voidedOn = voidedSalePayment.voidEntity.createdOn?.let { DateTimes.Local.atOrganizationZone(it) }
                    ?: DateTimes.Local.Now.organization()
            )
        )
    }
}
