package app.myliberty.easy_records.messaging.kafka.transaction.consumers

import app.myliberty.easy_records.messaging.kafka.common.KafkaConstants
import app.myliberty.easy_records.messaging.kafka.transaction.events.TransactionEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.util.enums.ServiceAccount
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class AccountingEventConsumer(
    private val accountingEventProcessors: List<AccountingEventProcessor<*>>,
    private val transactionEventConsumerSupport: TransactionEventConsumerSupport
) {

    @KafkaListener(
        topics = [KafkaConstants.Topics.TRANSACTION_EVENTS],
        groupId = KafkaConstants.ConsumerGroups.Transaction.ACCOUNTING
    )
    fun onTransactionEvent(event: TransactionEvent) {
        transactionEventConsumerSupport.consume(
            event,
            ServiceAccount.ACCOUNTING_PROCESSOR,
            KafkaConstants.ConsumerGroups.Transaction.ACCOUNTING,
            accountingEventProcessors
        )
    }
}
