package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.messaging.kafka.transaction.events.SaleVoidedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.SaleTaxLedgerEntriesBuilder
import app.myliberty.easy_records.util.business.Money
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class SaleVoidedEventProcessor(
    private val contactService: ContactService,
    private val ledgerPostingGate: LedgerPostingGate,
    private val saleTaxLedgerEntriesBuilder: SaleTaxLedgerEntriesBuilder,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SaleVoidedEvent>(ledgerPostingService) {

    override val eventType: KClass<SaleVoidedEvent> = SaleVoidedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SaleVoidedEvent): Boolean =
        ledgerPostingGate.isPosted(event.sourceContext, event.saleReferenceNumber, LedgerSourceType.SALE_VOID).not()

    override fun prepareLedgerRequest(event: SaleVoidedEvent): LedgerPostingRequest {
        ledgerPostingGate.requirePosted(event.sourceContext, event.saleReferenceNumber, LedgerSourceType.SALE)
        val contact = contactService.getContactById(event.contactId)

        val taxEntries = saleTaxLedgerEntriesBuilder.buildTransactionLevelReversalEntries(event.saleReferenceNumber)
        val saleEntries = buildList {
            addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.TRADE_RECEIVABLES.code, event.amountOwed.toBigDecimal()))
            addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.GROSS_SALES.code, event.grossRevenue.toBigDecimal()))
            addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.SALES_DISCOUNTS.code, event.discountTotal.toBigDecimal()))
            addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY.code, event.cogsTotal.toBigDecimal()))
            addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.COST_OF_GOODS_SOLD.code, event.cogsTotal.toBigDecimal()))
        }

        return LedgerPostingRequest(
            sourceReferenceNumber = event.saleReferenceNumber,
            sourceType = LedgerSourceType.SALE_VOID,
            postingDate = event.dateVoided,
            entries = saleEntries + taxEntries,
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contact.referenceNumber,
                    receivableAmount = Money.ZERO,
                    payableAmount = event.amountOwed
                )
            )
        )
    }
}
