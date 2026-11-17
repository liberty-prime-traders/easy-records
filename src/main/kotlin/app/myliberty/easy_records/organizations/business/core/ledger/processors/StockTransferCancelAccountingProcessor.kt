package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockTransferCancelledEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.StockTransferLedgerPostings
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class StockTransferCancelAccountingProcessor(
    private val stockTransferLedgerPostings: StockTransferLedgerPostings,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<StockTransferCancelledEvent>(ledgerPostingService) {

    override val eventType: KClass<StockTransferCancelledEvent> = StockTransferCancelledEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: StockTransferCancelledEvent): Boolean {
        val cancelCarriesCost = event.totalCost.toBigDecimal().signum() != 0
        val cancelAlreadyPosted = ledgerPostingGate.isPosted(
            event.sourceContext, event.dispatchReferenceNumber, LedgerSourceType.STOCK_TRANSFER_CANCEL
        )
        return cancelCarriesCost && !cancelAlreadyPosted
    }

    override fun prepareLedgerRequest(event: StockTransferCancelledEvent): LedgerPostingRequest {
        ledgerPostingGate.requirePosted(
            event.sourceContext, event.dispatchReferenceNumber, LedgerSourceType.STOCK_TRANSFER_DISPATCH
        )
        return LedgerPostingRequest(
            sourceReferenceNumber = event.dispatchReferenceNumber,
            sourceType = LedgerSourceType.STOCK_TRANSFER_CANCEL,
            postingDate = event.postingDate,
            entries = stockTransferLedgerPostings.moveOutOfTransit(event.totalCost),
            subledgerEntries = emptyList()
        )
    }
}
