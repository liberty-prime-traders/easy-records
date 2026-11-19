package app.myliberty.easy_records.messaging.kafka.transaction.processors

import app.myliberty.easy_records.messaging.kafka.transaction.events.TransactionEvent
import kotlin.reflect.KClass

sealed interface TransactionEventProcessor<EVENT : TransactionEvent> {
    val eventType: KClass<EVENT>

    // Name of the DB unique constraint backing this processor's idempotency, if any — lets the
    // consumer confirm a 23505 is that constraint firing on a lost race, not an unrelated violation.
    val idempotencyConstraintName: String? get() = null

    fun handle(event: EVENT)
    fun shouldProcess(event: EVENT): Boolean
}
