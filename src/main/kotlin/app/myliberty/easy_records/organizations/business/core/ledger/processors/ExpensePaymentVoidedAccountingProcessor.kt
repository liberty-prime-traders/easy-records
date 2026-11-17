package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.ExpensePostingRequests
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.reflect.KClass

@Service
class ExpensePaymentVoidedAccountingProcessor(
    contactService: ContactService,
    ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : ExpenseAccountingProcessor<ExpensePaymentVoidedEvent>(contactService, ledgerPostingGate, ledgerPostingService) {

    override val eventType: KClass<ExpensePaymentVoidedEvent> = ExpensePaymentVoidedEvent::class

    override val sourceType = LedgerSourceType.EXPENSE_PAYMENT_VOID

    override val reversedSourceType = LedgerSourceType.EXPENSE_PAYMENT

    override fun referenceNumberOf(event: ExpensePaymentVoidedEvent): String = event.paymentReferenceNumber

    override fun payeeContactIdOf(event: ExpensePaymentVoidedEvent): UUID = event.payeeContactId

    override fun buildRequest(event: ExpensePaymentVoidedEvent, payeeContactReferenceNumber: String): LedgerPostingRequest =
        ExpensePostingRequests.reverseSettle(
            sourceType = sourceType,
            paymentReference = event.paymentReferenceNumber,
            postingDate = event.voidedOn,
            expenseAccountCode = event.expenseAccountCode,
            paymentMethodAccountCode = event.paymentMethodAccountCode,
            amount = event.amount,
            payeeContactReferenceNumber = payeeContactReferenceNumber
        )
}
