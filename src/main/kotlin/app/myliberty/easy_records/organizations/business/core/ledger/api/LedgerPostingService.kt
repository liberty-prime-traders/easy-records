package app.myliberty.easy_records.organizations.business.core.ledger.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.core.account.api.AccountPosting
import app.myliberty.easy_records.organizations.business.core.account.api.AccountService
import app.myliberty.easy_records.organizations.business.core.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntriesValidator
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryEntity
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupEntity
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryRepository
import app.myliberty.easy_records.organizations.business.core.ledger.SubledgerBalanceRepository
import app.myliberty.easy_records.organizations.business.core.ledger.SubledgerEntryEntity
import app.myliberty.easy_records.organizations.business.core.ledger.SubledgerEntryRepository
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema
class LedgerPostingService(
    private val groupRepository: LedgerEntryGroupRepository,
    private val entryRepository: LedgerEntryRepository,
    private val subledgerRepository: SubledgerEntryRepository,
    private val subledgerBalanceRepository: SubledgerBalanceRepository,
    private val accountsAndLedgerLock: AccountsAndLedgerLock,
    private val fiscalPeriodService: FiscalPeriodService,
    private val accountService: AccountService,
) {

    fun post(ledgerPostingRequest: LedgerPostingRequest) {
        LedgerEntriesValidator.validate(ledgerPostingRequest.entries)
        accountsAndLedgerLock.acquire(ledgerPostingRequest)
        val accountPostings = ledgerPostingRequest.entries.map { ledgerEntryRequest ->
            AccountPosting(
                accountCode = ledgerEntryRequest.accountCode,
                amount = ledgerEntryRequest.amount.toBigDecimal(),
                entryType = ledgerEntryRequest.entryType
            )
        }
        if (ledgerPostingRequest.reversal) {
            accountService.assertPostableReversal(accountPostings)
        } else {
            accountService.assertPostable(accountPostings)
        }
        val group = saveLedgerGroup(ledgerPostingRequest)
        val groupReference = group.requiredReference()
        saveLedgerEntries(groupReference, ledgerPostingRequest)
        saveSubledgerEntries(groupReference, ledgerPostingRequest)
        accountService.patchBalances(accountPostings)
    }

    private fun saveLedgerGroup(ledgerPostingRequest: LedgerPostingRequest): LedgerEntryGroupEntity {
        val fiscalPeriodId = fiscalPeriodService.findOpenForDate(ledgerPostingRequest.postingDate)
            ?: throw LibertyGenericException("No open fiscal period for ${ledgerPostingRequest.postingDate}")
        val group = LedgerEntryGroupEntity(
            sourceReferenceNumber = ledgerPostingRequest.sourceReferenceNumber,
            sourceType = ledgerPostingRequest.sourceType,
            sourceLocationId = SessionContextProvider.getLocationIdOrNull(),
            fiscalPeriodId = fiscalPeriodId,
            postedOn = DateTimes.Offset.atStartOfDayInOrganizationZone(ledgerPostingRequest.postingDate).toInstant()
        )
        return groupRepository.save(group)
    }

    private fun saveLedgerEntries(
        groupReferenceNumber: String,
        ledgerPostingRequest: LedgerPostingRequest
    ) {
        entryRepository.saveAll(
            ledgerPostingRequest.entries.map { ledgerEntryRequest ->
                LedgerEntryEntity(
                    groupReferenceNumber = groupReferenceNumber,
                    accountCode = ledgerEntryRequest.accountCode,
                    entryType = ledgerEntryRequest.entryType,
                    amount = ledgerEntryRequest.amount.toBigDecimal()
                )
            }
        )
    }

    private fun saveSubledgerEntries(groupReferenceNumber: String, ledgerPostingRequest: LedgerPostingRequest) {
        if (ledgerPostingRequest.subledgerEntries.isEmpty()) return
        val contactReferenceNumbers = ledgerPostingRequest.subledgerEntries.map { it.contactReferenceNumber }.toSet()
        val balancesByContact = subledgerBalanceRepository.findByContactReferenceNumberIn(contactReferenceNumbers)
            .associateBy { it.contactReferenceNumber }

        subledgerRepository.saveAll(
            ledgerPostingRequest.subledgerEntries.map { subledgerEntryRequest ->
                val subledgerBalanceEntity = balancesByContact[subledgerEntryRequest.contactReferenceNumber]
                    ?: throw LibertyGenericException("No subledger balance exists for contact ${subledgerEntryRequest.contactReferenceNumber}")
                // Managed entity: dirty checking flushes the new totals with the entry rows.
                subledgerBalanceEntity.runningPayable += subledgerEntryRequest.payableAmount.toBigDecimal()
                subledgerBalanceEntity.runningReceivable += subledgerEntryRequest.receivableAmount.toBigDecimal()
                SubledgerEntryEntity(
                    groupReferenceNumber = groupReferenceNumber,
                    contactReferenceNumber = subledgerEntryRequest.contactReferenceNumber,
                    payableAmount = subledgerEntryRequest.payableAmount.toBigDecimal(),
                    receivableAmount = subledgerEntryRequest.receivableAmount.toBigDecimal(),
                    runningPayable = subledgerBalanceEntity.runningPayable,
                    runningReceivable = subledgerBalanceEntity.runningReceivable
                )
            }
        )
    }
}
