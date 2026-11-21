package app.myliberty.easy_records.messaging.kafka.transaction.processors

import app.myliberty.easy_records.messaging.kafka.transaction.events.TransactionEvent

interface InventoryEventProcessor<EVENT: TransactionEvent> : TransactionEventProcessor<EVENT>
