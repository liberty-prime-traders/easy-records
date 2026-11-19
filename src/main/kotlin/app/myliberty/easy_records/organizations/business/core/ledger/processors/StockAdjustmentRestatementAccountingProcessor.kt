package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockAdjustmentCostRestatedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.locations.business.retail.stock_revision.api.RevisionLedgerEntries
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.organizations.business.core.ledger.api.PostedStockAdjustmentAccountsReader
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockAdjustmentRestatementAccountingProcessor(
    private val ledgerPostingGate: LedgerPostingGate,
    private val postedStockAdjustmentAccountsReader: PostedStockAdjustmentAccountsReader,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<StockAdjustmentCostRestatedEvent>(ledgerPostingService) {

    override val eventType: KClass<StockAdjustmentCostRestatedEvent> = StockAdjustmentCostRestatedEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: StockAdjustmentCostRestatedEvent): Boolean =
        ledgerPostingGate.isPosted(
            event.sourceContext, event.stockRevisionReferenceNumber, LedgerSourceType.STOCK_ADJUSTMENT_RESTATEMENT
        ).not()

    override fun prepareLedgerRequest(event: StockAdjustmentCostRestatedEvent): LedgerPostingRequest =
        LedgerPostingRequest(
            sourceReferenceNumber = event.stockRevisionReferenceNumber,
            sourceType = LedgerSourceType.STOCK_ADJUSTMENT_RESTATEMENT,
            postingDate = event.postingDate,
            entries = entriesOf(event),
            subledgerEntries = emptyList()
        )

    private fun entriesOf(event: StockAdjustmentCostRestatedEvent): List<LedgerEntryRequest> {
        val adjustmentReferenceNumbers = event.restatements.map { it.adjustmentReferenceNumber }.toSet()
        val postedAccounts =
            postedStockAdjustmentAccountsReader.getByAdjustmentReferenceNumber(adjustmentReferenceNumbers)
        return RevisionLedgerEntries.stockAdjustmentRestatement(
            event.restatements.map { restatement ->
                RevisionLedgerEntries.adjustmentCostChangeOf(
                    restatement, postedAccounts[restatement.adjustmentReferenceNumber]
                )
            }
        )
    }
}
