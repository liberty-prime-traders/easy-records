package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.delivery.api.PurchaseDeliveryDataFetcher
import app.myliberty.easy_records.locations.business.purchase.api.PurchaseDataFetcher
import app.myliberty.easy_records.locations.business.stock.api.read.StockEntryFetcher
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveryRevisedEvent
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockRevisionHandlerForKafka(
    stockRevisionRepository: StockRevisionRepository,
    private val stockEntryFetcher: StockEntryFetcher,
    private val purchaseDeliveryDataFetcher: PurchaseDeliveryDataFetcher,
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val eventPublisher: ApplicationEventPublisher
) : StockRevisionEventHandler(stockRevisionRepository) {

    override val eventType = PurchaseDeliveryRevisedEvent::class

    override fun republish(stockRevision: StockRevisionEntity) {
        val revisedLineReferenceNumber = stockEntryFetcher.get(stockRevision.revisedStockEntryId).detailReference
            ?: throw RtsGenericException("Stock revision ${stockRevision.requiredReference()} has no revised line")
        val totals = purchaseDeliveryDataFetcher.getRevisionTotals(
            revisedLineReferenceNumber, stockRevision.sourceReferenceNumber
        )
        publish(stockRevision, totals.purchaseId, totals.revisedLineTotal, totals.replacementLineTotal)
    }

    // An unchanged line total moves no value, and the ledger rejects zero-amount entries.
    fun publish(
        stockRevision: StockRevisionEntity,
        purchaseId: UUID,
        revisedLineTotal: BigDecimal,
        replacementLineTotal: BigDecimal
    ) {
        if (revisedLineTotal.compareTo(replacementLineTotal) == 0) return
        val purchaseInfo = purchaseDataFetcher.findPurchaseInfoById(purchaseId)
        eventPublisher.publishEvent(
            PurchaseDeliveryRevisedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                stockRevisionId = stockRevision.id!!,
                stockRevisionReferenceNumber = stockRevision.requiredReference(),
                purchaseReferenceNumber = purchaseInfo.referenceNumber,
                supplierId = purchaseInfo.supplierId,
                revisedLineTotal = Money.of(revisedLineTotal),
                replacementLineTotal = Money.of(replacementLineTotal),
                postingDate = postingDate()
            )
        )
    }
}
