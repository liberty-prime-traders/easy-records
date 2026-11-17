package app.myliberty.easy_records.organizations.business.ledger.processors

import app.myliberty.easy_records.messaging.kafka.transaction.events.ExpenseRecordedEvent
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.ledger.helpers.ExpensePostingRequests
import app.myliberty.easy_records.organizations.business.ledger.helpers.LedgerPostingGate
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.reflect.KClass

@Service
class ExpenseRecordedAccountingProcessor(
    contactService: ContactService,
    ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : ExpenseAccountingProcessor<ExpenseRecordedEvent>(contactService, ledgerPostingGate, ledgerPostingService) {

    override val eventType: KClass<ExpenseRecordedEvent> = ExpenseRecordedEvent::class

    override val sourceType = LedgerSourceType.EXPENSE

    override fun referenceNumberOf(event: ExpenseRecordedEvent): String = event.expenseReferenceNumber

    override fun payeeContactIdOf(event: ExpenseRecordedEvent): UUID = event.payeeContactId

    override fun buildRequest(event: ExpenseRecordedEvent, payeeContactReferenceNumber: String): LedgerPostingRequest =
        ExpensePostingRequests.incur(
            sourceType = sourceType,
            reference = event.expenseReferenceNumber,
            postingDate = event.expenseDate,
            expenseAccountCode = event.expenseAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payeeContactReferenceNumber
        )
}
