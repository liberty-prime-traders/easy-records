package app.myliberty.easy_records.locations.business.retail.sale

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.SaleVoidedEvent
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SaleVoidHandlerForKafka(
    private val saleRepository: SaleRepository,
    private val saleVoidRepository: SaleVoidRepository,
    private val saleDataFetcher: SaleDataFetcher,
    private val eventPublisher: ApplicationEventPublisher,
) : EventReissueHandler {

    override val eventType = SaleVoidedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val sale = saleRepository.getReferenceById(sourceDocumentId)
        val voidEntity = saleVoidRepository.findBySaleId(sourceDocumentId)
            ?: throw LibertyGenericException("No sale_void record for sale $sourceDocumentId — cannot reissue SaleVoidedEvent")
        publishVoids(listOf(VoidedSale(sale, voidEntity)))
    }

    fun publishVoids(voidedSales: List<VoidedSale>) {
        val cogsTotalBySaleId = saleDataFetcher.getSaleCogsTotals(voidedSales.map { it.sale.id!! })
        voidedSales.forEach { voidedSale ->
            val sale = voidedSale.sale
            eventPublisher.publishEvent(
                SaleVoidedEvent(
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
                    dateVoided = voidedSale.voidDate,
                    cogsTotal = Money.of(cogsTotalBySaleId.getValue(sale.id!!))
                )
            )
        }
    }
}
