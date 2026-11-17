package app.myliberty.easy_records.locations.business.retail.stock_adjustment

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockAdjustmentRecordedEvent
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockAdjustmentHandlerForKafka(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val eventPublisher: ApplicationEventPublisher
) : EventReissueHandler {

    override val eventType = StockAdjustmentRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        val stockAdjustment = stockAdjustmentRepository.findById(sourceDocumentId)
            .orElseThrow { RtsGenericException("Stock adjustment $sourceDocumentId not found") }
        publish(stockAdjustment)
    }

    // Posts the original cost: a revision's restatement posts its own difference, so a reissue must not repeat it.
    // A zero-cost adjustment moves no value, and the ledger rejects zero-amount entries.
    fun publish(stockAdjustment: StockAdjustmentEntity) {
        if (stockAdjustment.totalCost.signum() == 0) return
        val ledgerAccounts = StockAdjustmentCategoryRules.ledgerAccounts(stockAdjustment.category, stockAdjustment.direction)
        eventPublisher.publishEvent(
            StockAdjustmentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = EventSourceContext.LocationLevel(
                    orgSchema = SessionContextProvider.getOrganizationSchema(),
                    locationSchema = SessionContextProvider.getLocationSchema()
                ),
                timestamp = Instant.now(),
                correlationId = null,
                stockAdjustmentId = requireNotNull(stockAdjustment.id),
                ledgerSourceReferenceNumber = stockAdjustment.requiredReference(),
                debitAccountCode = ledgerAccounts.debit.code,
                creditAccountCode = ledgerAccounts.credit.code,
                totalCost = Money.of(stockAdjustment.totalCost),
                postingDate = DateTimes.Local.atOrganizationZone(stockAdjustment.requiredCreatedOn())
            )
        )
    }
}
