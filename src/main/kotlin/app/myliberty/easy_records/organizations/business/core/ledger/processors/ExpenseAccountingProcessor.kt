package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.messaging.kafka.transaction.events.TransactionEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import java.util.UUID

abstract class ExpenseAccountingProcessor<EVENT : TransactionEvent>(
    private val contactService: ContactService,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<EVENT>(ledgerPostingService) {

    protected abstract val sourceType: LedgerSourceType

    // Set by voids: the posting they reverse, which shares the void's reference and differs only by source type.
    protected open val reversedSourceType: LedgerSourceType? = null

    protected abstract fun referenceNumberOf(event: EVENT): String

    protected abstract fun payeeContactIdOf(event: EVENT): UUID

    protected abstract fun buildRequest(event: EVENT, payeeContactReferenceNumber: String): LedgerPostingRequest

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: EVENT): Boolean =
        ledgerPostingGate.isPosted(event.sourceContext, referenceNumberOf(event), sourceType).not()

    override fun prepareLedgerRequest(event: EVENT): LedgerPostingRequest {
        reversedSourceType?.let { ledgerPostingGate.requirePosted(event.sourceContext, referenceNumberOf(event), it) }
        val payee = contactService.getContactById(payeeContactIdOf(event))
        return buildRequest(event, payee.referenceNumber)
    }
}
