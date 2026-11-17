package app.myliberty.easy_records.organizations.business.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockTransferDispatchedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.organizations.business.ledger.helpers.StockTransferLedgerPostings
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockTransferDispatchAccountingProcessor(
    private val stockTransferLedgerPostings: StockTransferLedgerPostings,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<StockTransferDispatchedEvent>(ledgerPostingService) {

    override val eventType: KClass<StockTransferDispatchedEvent> = StockTransferDispatchedEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: StockTransferDispatchedEvent): Boolean {
        val dispatchCarriesCost = event.totalCost.toBigDecimal().signum() != 0
        val dispatchAlreadyPosted = ledgerPostingGate.isPosted(
            event.sourceContext, event.dispatchReferenceNumber, LedgerSourceType.STOCK_TRANSFER_DISPATCH
        )
        return dispatchCarriesCost && !dispatchAlreadyPosted
    }

    override fun prepareLedgerRequest(event: StockTransferDispatchedEvent): LedgerPostingRequest =
        LedgerPostingRequest(
            sourceReferenceNumber = event.dispatchReferenceNumber,
            sourceType = LedgerSourceType.STOCK_TRANSFER_DISPATCH,
            postingDate = event.postingDate,
            entries = stockTransferLedgerPostings.moveIntoTransit(event.totalCost),
            subledgerEntries = emptyList()
        )
}
