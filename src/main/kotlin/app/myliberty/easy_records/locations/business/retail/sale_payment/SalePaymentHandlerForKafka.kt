package app.myliberty.easy_records.locations.business.sale_payment

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.sale.api.SaleDataFetcher
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.SalePaymentLineDto
import app.myliberty.easy_records.messaging.kafka.transaction.events.SalePaymentRecordedEvent
import app.myliberty.easy_records.util.business.PositiveMoney
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SalePaymentHandlerForKafka(
    private val saleDataFetcher: SaleDataFetcher,
    private val salePaymentRepository: SalePaymentRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SalePaymentRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) = publishExistingForSale(sourceDocumentId)

    fun publishExistingForSale(saleId: UUID) {
        val contactId = saleDataFetcher.getSaleContactId(saleId)
        val payments = salePaymentRepository.findBySaleId(saleId)
        if (payments.isEmpty()) return
        publishEvent(saleId, contactId, payments.map { toLine(it) })
    }

    fun publish(saleId: UUID, contactId: UUID, payments: List<SalePaymentEntity>) {
        if (payments.isNotEmpty()) {
            publishEvent(saleId, contactId, payments.map { toLine(it) })
        }
    }

    private fun toLine(payment: SalePaymentEntity) = SalePaymentLineDto(
        paymentReferenceNumber = payment.requiredReference(),
        paymentMethodAccountCode = payment.paymentMethodAccountCode,
        amount = PositiveMoney.of(payment.amount),
        paymentDate = payment.paymentDate
    )

    private fun publishEvent(saleId: UUID, contactId: UUID, payments: List<SalePaymentLineDto>) {
        eventPublisher.publishEvent(
            SalePaymentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = saleId,
                contactId = contactId,
                payments = payments
            )
        )
    }
}
