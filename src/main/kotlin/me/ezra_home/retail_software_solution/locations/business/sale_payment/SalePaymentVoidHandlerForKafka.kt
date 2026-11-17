package me.ezra_home.retail_software_solution.locations.business.sale_payment

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SalePaymentVoidedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
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
