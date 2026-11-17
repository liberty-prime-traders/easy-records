package app.myliberty.easy_records.organizations.business.ledger.processors

import app.myliberty.easy_records.messaging.kafka.transaction.events.ExpenseVoidedEvent
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
class ExpenseVoidedAccountingProcessor(
    contactService: ContactService,
    ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : ExpenseAccountingProcessor<ExpenseVoidedEvent>(contactService, ledgerPostingGate, ledgerPostingService) {

    override val eventType: KClass<ExpenseVoidedEvent> = ExpenseVoidedEvent::class

    override val sourceType = LedgerSourceType.EXPENSE_VOID

    override val reversedSourceType = LedgerSourceType.EXPENSE

    override fun referenceNumberOf(event: ExpenseVoidedEvent): String = event.expenseReferenceNumber

    override fun payeeContactIdOf(event: ExpenseVoidedEvent): UUID = event.payeeContactId

    override fun buildRequest(event: ExpenseVoidedEvent, payeeContactReferenceNumber: String): LedgerPostingRequest =
        ExpensePostingRequests.reverseIncur(
            sourceType = sourceType,
            reference = event.expenseReferenceNumber,
            postingDate = event.voidedOn,
            expenseAccountCode = event.expenseAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payeeContactReferenceNumber
        )
}
