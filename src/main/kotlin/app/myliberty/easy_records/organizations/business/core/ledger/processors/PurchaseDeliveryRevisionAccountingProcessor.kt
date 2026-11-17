package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveryRevisedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.locations.business.retail.stock_revision.api.RevisionLedgerEntries
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.util.business.Money
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class PurchaseDeliveryRevisionAccountingProcessor(
    private val contactService: ContactService,
    private val ledgerPostingGate: LedgerPostingGate,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<PurchaseDeliveryRevisedEvent>(ledgerPostingService) {

    override val eventType: KClass<PurchaseDeliveryRevisedEvent> = PurchaseDeliveryRevisedEvent::class

    override val idempotencyConstraintName: String get() = ledgerPostingGate.idempotencyConstraintName()

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: PurchaseDeliveryRevisedEvent): Boolean =
        ledgerPostingGate.isPosted(
            event.sourceContext, event.stockRevisionReferenceNumber, LedgerSourceType.PURCHASE_DELIVERY_REVISION
        ).not()

    override fun prepareLedgerRequest(event: PurchaseDeliveryRevisedEvent): LedgerPostingRequest {
        val supplier = contactService.getContactById(event.supplierId)
        val revisedLineTotal = event.revisedLineTotal.toBigDecimal()
        val replacementLineTotal = event.replacementLineTotal.toBigDecimal()
        val changeInLineTotal = replacementLineTotal.subtract(revisedLineTotal)
        val increasesAmountOwed = changeInLineTotal.signum() > 0
        val amount = changeInLineTotal.abs()

        return LedgerPostingRequest(
            sourceReferenceNumber = event.stockRevisionReferenceNumber,
            sourceType = LedgerSourceType.PURCHASE_DELIVERY_REVISION,
            postingDate = event.postingDate,
            entries = RevisionLedgerEntries.purchaseDeliveryRevision(
                revisedLineTotal, replacementLineTotal
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = supplier.referenceNumber,
                    payableAmount = if (increasesAmountOwed) Money.of(amount) else Money.ZERO,
                    receivableAmount = if (increasesAmountOwed) Money.ZERO else Money.of(amount)
                )
            )
        )
    }
}
