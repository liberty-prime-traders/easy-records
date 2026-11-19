package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.messaging.kafka.transaction.events.SupplierPaymentVoidedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.util.business.Money
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class SupplierPaymentVoidAccountingProcessor(
    private val contactService: ContactService,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SupplierPaymentVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<SupplierPaymentVoidedEvent> = SupplierPaymentVoidedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SupplierPaymentVoidedEvent): Boolean =
        ledgerPostingGate.isPosted(event.sourceContext, event.paymentReferenceNumber, LedgerSourceType.SUPPLIER_PAYMENT_VOID).not()

    override fun prepareLedgerRequest(event: SupplierPaymentVoidedEvent): LedgerPostingRequest {
        ledgerPostingGate.requirePosted(event.sourceContext, event.paymentReferenceNumber, LedgerSourceType.SUPPLIER_PAYMENT)
        val supplier = contactService.getContactById(event.supplierId)

        return LedgerPostingRequest(
            sourceReferenceNumber = event.paymentReferenceNumber,
            sourceType = LedgerSourceType.SUPPLIER_PAYMENT_VOID,
            postingDate = event.voidedOn,
            entries = listOf(
                LedgerEntryRequest.credit(SystemAccount.TRADE_PAYABLES.code, event.amount),
                LedgerEntryRequest.debit(event.paymentMethodAccountCode, event.amount)
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = supplier.referenceNumber,
                    payableAmount = Money.of(event.amount.toBigDecimal()),
                    receivableAmount = Money.ZERO
                )
            )
        )
    }
}
