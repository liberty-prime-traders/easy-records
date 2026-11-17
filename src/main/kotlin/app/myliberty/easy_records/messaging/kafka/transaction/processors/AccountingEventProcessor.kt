package app.myliberty.easy_records.messaging.kafka.transaction.processors

import app.myliberty.easy_records.messaging.kafka.transaction.events.TransactionEvent
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService

abstract class AccountingEventProcessor<EVENT : TransactionEvent>(
    private val ledgerPostingService: LedgerPostingService
) : TransactionEventProcessor<EVENT> {

    open fun prepareLedgerRequest(event: EVENT): LedgerPostingRequest? = null

    open fun prepareLedgerRequests(event: EVENT): List<LedgerPostingRequest> =
        listOfNotNull(prepareLedgerRequest(event))

    override fun handle(event: EVENT) {
        prepareLedgerRequests(event).forEach { ledgerPostingService.post(it) }
    }
}
