package app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactStatus
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningBalanceEntity
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningBalancePostingRequests
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningBalanceRepository
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.ContactOpeningSettlementFinder
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerBalanceDto
import app.myliberty.easy_records.organizations.business.core.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.DisplayFormatters
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.ui_models.BalanceEffect
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class ContactOpeningBalanceService(
    private val contactOpeningBalanceRepository: ContactOpeningBalanceRepository,
    private val contactService: ContactService,
    private val ledgerPostingService: LedgerPostingService,
    private val contactOpeningSettlementFinder: ContactOpeningSettlementFinder,
    private val orgEntityAdvisoryLock: OrgEntityAdvisoryLock,
    private val fiscalPeriodService: FiscalPeriodService,
    private val userQualifier: UserQualifier
) {

    fun set(contactId: UUID, contactOpeningBalanceSetDto: ContactOpeningBalanceSetDto) {
        val postingDate = DateTimes.Local.Now.organization()
        fiscalPeriodService.requireOpenForDate(postingDate)
        val contactDto = contactService.getContactById(contactId)
        if (contactDto.status != ContactStatus.ACTIVE) {
            throw LibertyGenericException("Only an active contact can take an opening balance")
        }
        orgEntityAdvisoryLock.acquire(LockNamespaces.CONTACT_OPENING_BALANCE, contactDto.referenceNumber)
        val existingBalance = SubledgerBalanceDto(
            payable = contactOpeningSettlementFinder.declaredAmount(
                contactDto.referenceNumber,
                ContactSubledgerDirection.PAYABLE
            ),
            receivable = contactOpeningSettlementFinder.declaredAmount(
                contactDto.referenceNumber,
                ContactSubledgerDirection.RECEIVABLE
            )
        )
        val targetBalance = contactOpeningBalanceSetDto.applyTo(existingBalance)
        requireNotBelowSettled(
            contactDto.referenceNumber,
            ContactSubledgerDirection.RECEIVABLE,
            targetBalance.receivable
        )
        requireNotBelowSettled(
            contactDto.referenceNumber,
            ContactSubledgerDirection.PAYABLE,
            targetBalance.payable
        )
        reviseSubledgerDirection(
            contactDto.referenceNumber, ContactSubledgerDirection.RECEIVABLE,
            existingBalance.receivable, targetBalance.receivable, postingDate
        )
        reviseSubledgerDirection(
            contactDto.referenceNumber, ContactSubledgerDirection.PAYABLE,
            existingBalance.payable, targetBalance.payable, postingDate
        )
    }

    private fun requireNotBelowSettled(
        contactReferenceNumber: String,
        subledgerDirection: ContactSubledgerDirection,
        targetAmount: BigDecimal
    ) {
        val settledAmount = contactOpeningSettlementFinder.activeSettledAmount(contactReferenceNumber, subledgerDirection)
        if (targetAmount < settledAmount) {
            throw LibertyGenericException(
                "The ${subledgerDirection.name.lowercase()} opening balance cannot be set to " +
                    "${DisplayFormatters.formatCurrency(targetAmount)}, below the " +
                    "${DisplayFormatters.formatCurrency(settledAmount)} already settled against it; " +
                    "void the settlements first"
            )
        }
    }

    private fun reviseSubledgerDirection(
        contactReferenceNumber: String,
        subledgerDirection: ContactSubledgerDirection,
        existingAmount: BigDecimal,
        targetAmount: BigDecimal,
        postingDate: LocalDate
    ) {
        val signedDelta = targetAmount - existingAmount
        if (signedDelta.compareTo(BigDecimal.ZERO) == 0) return

        val savedContactOpeningBalanceEntity = contactOpeningBalanceRepository.save(
            ContactOpeningBalanceEntity(
                contactReferenceNumber = contactReferenceNumber,
                subledgerDirection = subledgerDirection,
                amount = targetAmount
            )
        )
        ledgerPostingService.post(
            ContactOpeningBalancePostingRequests.revised(
                ledgerSourceReferenceNumber = savedContactOpeningBalanceEntity.requiredReference(),
                contactReferenceNumber = contactReferenceNumber,
                subledgerDirection = subledgerDirection,
                signedDelta = signedDelta,
                postingDate = postingDate
            )
        )
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getHistory(contactReferenceNumber: String): List<ContactOpeningBalanceRevisionDto> =
        contactOpeningBalanceRepository.findByContactReferenceNumberOrderByCreatedOnAsc(contactReferenceNumber)
            .map { revisionRow ->
                ContactOpeningBalanceRevisionDto(
                    referenceNumber = revisionRow.requiredReference(),
                    contactReferenceNumber = revisionRow.contactReferenceNumber,
                    subledgerDirection = revisionRow.subledgerDirection,
                    amount = revisionRow.amount,
                    balanceEffect = claimedBalanceEffect(revisionRow),
                    changedAt = revisionRow.requiredCreatedOn(),
                    changedBy = userQualifier.getUserFullName(revisionRow.createdById) ?: ""
                )
            }

    private fun claimedBalanceEffect(revisionRow: ContactOpeningBalanceEntity): BalanceEffect = BalanceEffect.ofChange(
        revisionRow.amount,
        if (revisionRow.subledgerDirection == ContactSubledgerDirection.RECEIVABLE) BalanceEffect.ADDS else BalanceEffect.SUBTRACTS
    )
}
