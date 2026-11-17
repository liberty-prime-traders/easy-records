package app.myliberty.easy_records.cross_tier.expense.kafka_handler

import app.myliberty.easy_records.messaging.kafka.transaction.EventReissueHandler
import app.myliberty.easy_records.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ExpensePaymentRecordedHandlerForKafka(
    private val expenseReissuerResolver: ExpenseReissuerResolver
) : EventReissueHandler {

    override val eventType = ExpensePaymentRecordedEvent::class

    override fun reissue(sourceDocumentId: UUID) {
        expenseReissuerResolver.current().reissuePaymentRecorded(sourceDocumentId)
    }
}
