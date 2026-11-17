package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.stock.api.read.StockMovementFetcher
import app.myliberty.easy_records.messaging.kafka.transaction.events.SaleCostRestatedEvent
import app.myliberty.easy_records.util.business.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
@TransactionalOnLocationSchema(readOnly = true)
class SaleCostRestatementHandlerForKafka(
    stockRevisionRepository: StockRevisionRepository,
    private val stockMovementFetcher: StockMovementFetcher,
    private val eventPublisher: ApplicationEventPublisher
) : StockRevisionEventHandler(stockRevisionRepository) {

    override val eventType = SaleCostRestatedEvent::class

    override fun republish(stockRevision: StockRevisionEntity) {
        val revisionMovements = stockMovementFetcher.findOfRevision(stockRevision.requiredReference())
        publish(stockRevision, ConsumerCostChange.ofRevisionMovements(revisionMovements))
    }

    // Every draw's cost is already at money scale, so the totals are exact and Money.of rejects anything finer.
    fun publish(stockRevision: StockRevisionEntity, consumerCostChanges: List<ConsumerCostChange>) {
        val saleCostChanges = RestatableConsumers.salesOf(consumerCostChanges)
        val oldCostTotal = saleCostChanges.sumOf { it.oldCost }
        val newCostTotal = saleCostChanges.sumOf { it.newCost }
        if (oldCostTotal.compareTo(newCostTotal) == 0) return
        eventPublisher.publishEvent(
            SaleCostRestatedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                stockRevisionId = stockRevision.id!!,
                stockRevisionReferenceNumber = stockRevision.requiredReference(),
                oldCostTotal = Money.of(oldCostTotal),
                newCostTotal = Money.of(newCostTotal),
                postingDate = postingDate()
            )
        )
    }
}
