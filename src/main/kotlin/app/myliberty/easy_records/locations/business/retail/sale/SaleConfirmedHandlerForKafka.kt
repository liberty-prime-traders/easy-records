package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.SaleConfirmedEvent
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SaleConfirmedHandlerForKafka(
    private val saleRepository: SaleRepository,
    private val saleDataFetcher: SaleDataFetcher,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = SaleConfirmedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        publish(saleRepository.getReferenceById(sourceDocumentId))
    }

    fun publish(sale: SaleEntity) {
        val cogsTotal = saleDataFetcher.getSaleCogsTotals(listOf(sale.id!!)).getValue(sale.id!!)
        eventPublisher.publishEvent(
            SaleConfirmedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                sourceDocumentId = sale.id!!,
                contactId = sale.contactId,
                saleReferenceNumber = sale.requiredReference(),
                taxableAmount = Money.of(sale.taxableAmount()),
                taxBilled = Money.of(sale.taxBilled),
                discountTotal = Money.of(sale.discountTotal()),
                dateSold = DateTimes.Local.atOrganizationZone(sale.dateSold!!),
                cogsTotal = Money.of(cogsTotal)
            )
        )
    }
}
