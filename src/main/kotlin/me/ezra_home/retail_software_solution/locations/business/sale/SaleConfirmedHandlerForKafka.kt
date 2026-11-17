package me.ezra_home.retail_software_solution.locations.business.sale

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.EventReissueHandler
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.SaleConfirmedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.business.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigDecimal
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
        publish(listOf(saleRepository.getReferenceById(sourceDocumentId)))
    }

    fun publish(sale: SaleEntity) = publish(listOf(sale))

    fun publish(sales: List<SaleEntity>) {
        val cogsTotalBySaleId = saleDataFetcher.getSaleCogsTotals(sales.map { it.id!! })
        sales.forEach { publishEvent(it, cogsTotalBySaleId.getValue(it.id!!)) }
    }

    private fun publishEvent(sale: SaleEntity, cogsTotal: BigDecimal) {
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
