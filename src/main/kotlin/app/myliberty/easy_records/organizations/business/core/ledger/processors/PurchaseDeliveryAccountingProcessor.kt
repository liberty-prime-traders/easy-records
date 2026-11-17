package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerEntryRequest
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class PurchaseDeliveryAccountingProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<PurchaseDeliveredEvent>(ledgerPostingService) {

    override val eventType: KClass<PurchaseDeliveredEvent> = PurchaseDeliveredEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: PurchaseDeliveredEvent): Boolean =
        ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(
            event.deliveryReferenceNumber,
            SessionContextProvider.getLocationId()
        ).not()

    override fun prepareLedgerRequest(event: PurchaseDeliveredEvent): LedgerPostingRequest {
        val postingDate = DateTimes.Local.atOrganizationZone(event.deliveredAt)

        return LedgerPostingRequest(
            sourceReferenceNumber = event.deliveryReferenceNumber,
            sourceType = LedgerSourceType.PURCHASE_DELIVERY,
            postingDate = postingDate,
            entries = getLedgerEntries(event),
            subledgerEntries = getSubLedgerEntries(event)
        )
    }

    private fun getLedgerEntries(event: PurchaseDeliveredEvent): List<LedgerEntryRequest> {
        val total = event.deliveryTotal.toBigDecimal()
        return LedgerEntryRequest.debitUnlessZero(SystemAccount.INVENTORY.code, total) +
            LedgerEntryRequest.creditUnlessZero(SystemAccount.TRADE_PAYABLES.code, total)
    }
    private fun getSubLedgerEntries(event: PurchaseDeliveredEvent): List<SubledgerEntryRequest> {
        val supplier = contactService.getContactById(event.supplierId)

        return listOf(
            SubledgerEntryRequest(
                contactReferenceNumber = supplier.referenceNumber,
                payableAmount = event.deliveryTotal,
                receivableAmount = Money.ZERO
            )
        )
    }
}
