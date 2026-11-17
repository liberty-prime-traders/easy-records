package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.stock.api.read.StockMovementFetcher
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockAdjustmentCostRestatedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class StockAdjustmentRestatementHandlerForKafka(
    stockRevisionRepository: StockRevisionRepository,
    private val stockMovementFetcher: StockMovementFetcher,
    private val stockAdjustmentRestatements: StockAdjustmentRestatements,
    private val eventPublisher: ApplicationEventPublisher
) : StockRevisionEventHandler(stockRevisionRepository) {

    override val eventType = StockAdjustmentCostRestatedEvent::class

    override fun republish(stockRevision: StockRevisionEntity) {
        val revisionMovements = stockMovementFetcher.findOfRevision(stockRevision.requiredReference())
        publish(stockRevision, ConsumerCostChange.ofRevisionMovements(revisionMovements))
    }

    fun publish(stockRevision: StockRevisionEntity, consumerCostChanges: List<ConsumerCostChange>) {
        val restatements = stockAdjustmentRestatements.of(consumerCostChanges)
        if (restatements.isEmpty()) return
        eventPublisher.publishEvent(
            StockAdjustmentCostRestatedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                stockRevisionId = stockRevision.id!!,
                stockRevisionReferenceNumber = stockRevision.requiredReference(),
                restatements = restatements,
                postingDate = postingDate()
            )
        )
    }
}
