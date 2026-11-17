package app.myliberty.easy_records.organizations.business.ledger.processors

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.transaction.events.SalePaymentLineDto
import app.myliberty.easy_records.messaging.kafka.transaction.events.SalePaymentRecordedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.processors.AccountingEventProcessor
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.ledger.api.SubledgerEntryRequest
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.Money
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Service
class SalePaymentRecordedEventProcessor(
    private val contactService: ContactService,
    private val ledgerEntryGroupRepository: LedgerEntryGroupRepository,
    ledgerPostingService: LedgerPostingService
) : AccountingEventProcessor<SalePaymentRecordedEvent>(ledgerPostingService) {

    override val eventType: KClass<SalePaymentRecordedEvent> = SalePaymentRecordedEvent::class

    @TransactionalOnOrganizationSchema(readOnly = true)
    override fun shouldProcess(event: SalePaymentRecordedEvent): Boolean {
        val processedPayments = getProcessedPayments(event.payments)
        return processedPayments.size != event.payments.size
    }

    private fun getProcessedPayments(payments: List<SalePaymentLineDto>): Set<String> {
        val locationId = SessionContextProvider.getLocationId()
        return payments
            .filter { ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId(it.paymentReferenceNumber, locationId) }
            .map { it.paymentReferenceNumber }
            .toSet()
    }

    override fun prepareLedgerRequests(event: SalePaymentRecordedEvent): List<LedgerPostingRequest> {
        val contactReferenceNumber = contactService.getContactById(event.contactId).referenceNumber
        val processedPayments = getProcessedPayments(event.payments)
        return event.payments
            .filter { it.paymentReferenceNumber !in processedPayments }
            .map { buildRequest(it, contactReferenceNumber) }
    }

    private fun buildRequest(payment: SalePaymentLineDto, contactReferenceNumber: String) =
        LedgerPostingRequest(
            sourceReferenceNumber = payment.paymentReferenceNumber,
            sourceType = LedgerSourceType.SALE_PAYMENT,
            postingDate = DateTimes.Local.atOrganizationZone(payment.paymentDate),
            entries = listOf(
                LedgerEntryRequest.debit(payment.paymentMethodAccountCode, payment.amount),
                LedgerEntryRequest.credit(SystemAccount.TRADE_RECEIVABLES.code, payment.amount)
            ),
            subledgerEntries = listOf(
                SubledgerEntryRequest(
                    contactReferenceNumber = contactReferenceNumber,
                    receivableAmount = Money.ZERO,
                    payableAmount = Money.of(payment.amount.toBigDecimal())
                )
            )
        )
}
