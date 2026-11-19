package app.myliberty.easy_records.cross_tier.core.expense.kafka_handler

import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.ExpenseVoidedEvent
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpenseVoidedHandlerForKafka(
    private val expenseReissuerResolver: ExpenseReissuerResolver
) : EventReissueHandler {

    override val eventType = ExpenseVoidedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        expenseReissuerResolver.current().reissueVoided(sourceDocumentId)
    }
}
