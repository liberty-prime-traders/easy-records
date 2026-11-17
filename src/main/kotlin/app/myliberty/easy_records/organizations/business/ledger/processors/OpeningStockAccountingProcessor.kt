package app.myliberty.easy_records.organizations.business.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.transaction.events.OpeningStockDeclaredEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class OpeningStockAccountingProcessor(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<OpeningStockDeclaredEvent>(ledgerPostingService) {

    override val eventType: KClass<OpeningStockDeclaredEvent> = OpeningStockDeclaredEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: OpeningStockDeclaredEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(
            event.ledgerSourceReferenceNumber,
            SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: OpeningStockDeclaredEvent): LedgerPostingRequest {
        val amount = event.totalCost.toBigDecimal()
        return LedgerPostingRequest(
            sourceReferenceNumber = event.ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.OPENING_STOCK,
            postingDate = event.postingDate,
            entries = LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY.code, amount) +
                LedgerEntryRequest.creditUnlessZero(SystemAccount.OPENING_BALANCE_EQUITY.code, amount),
            subledgerEntries = emptyList()
        )
    }
}
