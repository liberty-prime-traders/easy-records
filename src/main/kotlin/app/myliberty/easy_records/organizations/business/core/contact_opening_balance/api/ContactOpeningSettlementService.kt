package app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactStatus
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningSettlementEntity
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningSettlementFinder
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningSettlementPostingRequests
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningSettlementRepository
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningSettlementResponseBuilder
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningSettlementVoidEntity
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningSettlementVoidRepository
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.organizations.business.core.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.DisplayFormatters
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class ContactOpeningSettlementService(
    private val contactOpeningSettlementRepository: ContactOpeningSettlementRepository,
    private val contactOpeningSettlementVoidRepository: ContactOpeningSettlementVoidRepository,
    private val contactOpeningSettlementFinder: ContactOpeningSettlementFinder,
    private val contactOpeningSettlementResponseBuilder: ContactOpeningSettlementResponseBuilder,
    private val contactService: ContactService,
    private val paymentMethodDataFetcher: PaymentMethodDataFetcher,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock,
    private val ledgerPostingService: LedgerPostingService
) {

    fun recordSettlement(
        contactOpeningSettlementCreateRequest: ContactOpeningSettlementCreateRequest
    ): ContactOpeningSettlementSummaryResponse {
        DateTimes.Local.requireNotInFuture(contactOpeningSettlementCreateRequest.settlementDate, "Settlement date")
        val contactDto = contactService.getContactById(contactOpeningSettlementCreateRequest.contactId)
        if (contactDto.status != ContactStatus.ACTIVE) {
            throw RtsGenericException("Only an active contact can be settled against an opening balance")
        }
        orgEntityAdvisoryLock.acquire(LockNamespaces.CONTACT_OPENING_BALANCE, contactDto.referenceNumber)
        val settlementAmount = contactOpeningSettlementCreateRequest.amount.toBigDecimal()
        val outstandingAmount = contactOpeningSettlementFinder.outstandingAmount(
            contactDto.referenceNumber,
            contactOpeningSettlementCreateRequest.subledgerDirection
        )
        if (settlementAmount > outstandingAmount) {
            val directionName = contactOpeningSettlementCreateRequest.subledgerDirection.name.lowercase()
            throw RtsGenericException(
                "Settlement of ${DisplayFormatters.formatCurrency(settlementAmount)} exceeds the outstanding " +
                    "${DisplayFormatters.formatCurrency(outstandingAmount)} $directionName " +
                    "opening balance of ${contactDto.referenceNumber}; declare or raise the opening balance first"
            )
        }
        val savedSettlementEntity = contactOpeningSettlementRepository.saveAndFlush(
            ContactOpeningSettlementEntity(
                contactReferenceNumber = contactDto.referenceNumber,
                subledgerDirection = contactOpeningSettlementCreateRequest.subledgerDirection,
                paymentAccountCode = paymentMethodDataFetcher
                    .findAccountCode(contactOpeningSettlementCreateRequest.paymentMethodId),
                amount = settlementAmount,
                settlementDate = contactOpeningSettlementCreateRequest.settlementDate,
                note = StringUtils.getValueOrNull(contactOpeningSettlementCreateRequest.note)
            )
        )
        ledgerPostingService.post(ContactOpeningSettlementPostingRequests.recorded(savedSettlementEntity))
        return contactOpeningSettlementResponseBuilder.buildSummary(contactDto.referenceNumber)
    }

    fun voidSettlement(
        contactOpeningSettlementVoidRequest: ContactOpeningSettlementVoidRequest
    ): ContactOpeningSettlementSummaryResponse {
        val settlementEntity = contactOpeningSettlementRepository
            .findById(contactOpeningSettlementVoidRequest.settlementId)
            .orElseThrow { RtsGenericException("Contact opening settlement not found") }
        orgEntityAdvisoryLock.acquire(LockNamespaces.CONTACT_OPENING_BALANCE, settlementEntity.contactReferenceNumber)
        if (contactOpeningSettlementVoidRepository.existsBySettlementId(settlementEntity.requiredId())) {
            throw RtsGenericException(
                "Contact opening settlement ${settlementEntity.requiredReference()} is already voided"
            )
        }
        val voidDate = DateTimes.Local.Now.organization()
        contactOpeningSettlementVoidRepository.saveAndFlush(
            ContactOpeningSettlementVoidEntity(
                settlementId = settlementEntity.requiredId(),
                voidDate = voidDate,
                reason = StringUtils.getValueOrNull(contactOpeningSettlementVoidRequest.reason)
            )
        )
        ledgerPostingService.post(ContactOpeningSettlementPostingRequests.voided(settlementEntity, voidDate))
        return contactOpeningSettlementResponseBuilder.buildSummary(settlementEntity.contactReferenceNumber)
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getSummary(contactReferenceNumber: String): ContactOpeningSettlementSummaryResponse =
        contactOpeningSettlementResponseBuilder.buildSummary(contactReferenceNumber)
}
