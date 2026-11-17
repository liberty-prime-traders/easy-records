package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.ExpensePostingRequests
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.util.business.DateTimes
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.reflect.KClass

@Service
class ExpensePaymentRecordedAccountingProcessor(
    contactService: ContactService,
    ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : ExpenseAccountingProcessor<ExpensePaymentRecordedEvent>(contactService, ledgerPostingGate, ledgerPostingService) {

    override val eventType: KClass<ExpensePaymentRecordedEvent> = ExpensePaymentRecordedEvent::class

    override val sourceType = LedgerSourceType.EXPENSE_PAYMENT

    override fun referenceNumberOf(event: ExpensePaymentRecordedEvent): String = event.paymentReferenceNumber

    override fun payeeContactIdOf(event: ExpensePaymentRecordedEvent): UUID = event.payeeContactId

    override fun buildRequest(event: ExpensePaymentRecordedEvent, payeeContactReferenceNumber: String): LedgerPostingRequest =
        ExpensePostingRequests.settle(
            sourceType = sourceType,
            paymentReference = event.paymentReferenceNumber,
            postingDate = DateTimes.Local.atOrganizationZone(event.paymentDate),
            expenseAccountCode = event.expenseAccountCode,
            paymentMethodAccountCode = event.paymentMethodAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payeeContactReferenceNumber
        )
}
