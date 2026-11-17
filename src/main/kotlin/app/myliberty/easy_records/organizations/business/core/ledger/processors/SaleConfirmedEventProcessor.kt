package app.myliberty.easy_records.organizations.business.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.transaction.events.SaleConfirmedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.ledger.api.SubledgerEntryRequest
import app.myliberty.easy_records.organizations.business.ledger.helpers.SaleTaxLedgerEntriesBuilder
import app.myliberty.easy_records.util.business.Money
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class SaleConfirmedEventProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    private val saleTaxLedgerEntriesBuilder: SaleTaxLedgerEntriesBuilder,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SaleConfirmedEvent>(ledgerPostingService) {

    override val eventType: KClass<SaleConfirmedEvent> = SaleConfirmedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SaleConfirmedEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(
            event.saleReferenceNumber, SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: SaleConfirmedEvent): LedgerPostingRequest {
        val contact = contactService.getContactById(event.contactId)

        val ledgerEntries = buildList {
            addAll(saleTaxLedgerEntriesBuilder.buildTransactionLevelEntries(event.saleReferenceNumber))
            addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.TRADE_RECEIVABLES.code, event.amountOwed.toBigDecimal()))
            addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.GROSS_SALES.code, event.grossRevenue.toBigDecimal()))
            addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.SALES_DISCOUNTS.code, event.discountTotal.toBigDecimal()))
            addAll(LedgerEntryRequest.debitUnlessZero(SystemAccount.COST_OF_GOODS_SOLD.code, event.cogsTotal.toBigDecimal()))
            addAll(LedgerEntryRequest.creditUnlessZero(SystemAccount.INVENTORY.code, event.cogsTotal.toBigDecimal()))
        }

        return LedgerPostingRequest(
            sourceReferenceNumber = event.saleReferenceNumber,
            sourceType = LedgerSourceType.SALE,
            postingDate = event.dateSold,
            entries = ledgerEntries,
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contact.referenceNumber,
                    receivableAmount = event.amountOwed,
                    payableAmount = Money.ZERO
                )
            )
        )
    }

}
