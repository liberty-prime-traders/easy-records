package app.myliberty.easy_records.organizations.business.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockAdjustmentRecordedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockAdjustmentAccountingProcessor(
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<StockAdjustmentRecordedEvent>(ledgerPostingService) {

    override val eventType: KClass<StockAdjustmentRecordedEvent> = StockAdjustmentRecordedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: StockAdjustmentRecordedEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(
            event.ledgerSourceReferenceNumber,
            SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: StockAdjustmentRecordedEvent): LedgerPostingRequest {
        return LedgerPostingRequest(
            sourceReferenceNumber = event.ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.STOCK_ADJUSTMENT,
            postingDate = event.postingDate,
            entries = LedgerEntryRequest.debitUnlessZero(event.debitAccountCode, event.totalCost.toBigDecimal()) +
                LedgerEntryRequest.creditUnlessZero(event.creditAccountCode, event.totalCost.toBigDecimal()),
            subledgerEntries = emptyList()
        )
    }
}
