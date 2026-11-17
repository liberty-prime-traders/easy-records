package app.myliberty.easy_records.organizations.business.opening_balance.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.organizations.business.account.api.AccountDataFetcher
import app.myliberty.easy_records.organizations.business.account.api.AccountStructureLock
import app.myliberty.easy_records.organizations.business.fiscal_period.api.FiscalPeriodService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.opening_balance.OpeningBalanceAccountValidator
import app.myliberty.easy_records.organizations.business.opening_balance.OpeningBalanceEntity
import app.myliberty.easy_records.organizations.business.opening_balance.OpeningBalancePostingRequests
import app.myliberty.easy_records.organizations.business.opening_balance.OpeningBalanceRepository
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.mappers.UserQualifier
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnOrganizationSchema
class OpeningBalanceService(
    private val openingBalanceRepository: OpeningBalanceRepository,
    private val accountDataFetcher: AccountDataFetcher,
    private val ledgerPostingService: LedgerPostingService,
    private val accountStructureLock: AccountStructureLock,
    private val fiscalPeriodService: FiscalPeriodService,
    private val userQualifier: UserQualifier
) {

    fun upsert(upsertDto: OpeningBalanceUpsertDto) {
        OpeningBalanceAccountValidator.assertManualOpeningBalanceAccepted(upsertDto.accountCode)
        val postingDate = DateTimes.Local.Now.organization()
        fiscalPeriodService.requireOpenForDate(postingDate)
        accountStructureLock.acquire(upsertDto.accountCode)
        val account = accountDataFetcher.getByCode(upsertDto.accountCode)
        OpeningBalanceAccountValidator.requireLeafActive(account)
        val latest = openingBalanceRepository.findLatestForAccountCode(upsertDto.accountCode)
        val delta = upsertDto.newAmount.toBigDecimal() - (latest?.amount ?: BigDecimal.ZERO)
        if (delta.compareTo(BigDecimal.ZERO) == 0) return

        val saved = openingBalanceRepository.save(
            OpeningBalanceEntity(accountCode = upsertDto.accountCode, amount = upsertDto.newAmount.toBigDecimal())
        )
        ledgerPostingService.post(
            OpeningBalancePostingRequests.revised(
                ledgerSourceReferenceNumber = saved.requiredReference(),
                accountCode = upsertDto.accountCode,
                normalBalanceEntryType = account.normalBalanceEntryType,
                signedDelta = delta,
                postingDate = postingDate
            )
        )
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getHistory(accountCode: String): List<OpeningBalanceRevisionDto> {
        val rows = openingBalanceRepository.findByAccountCodeOrderByCreatedOnAsc(accountCode)
        return rows.map { row ->
            OpeningBalanceRevisionDto(
                referenceNumber = row.requiredReference(),
                accountCode = row.accountCode,
                amount = row.amount,
                changedBy = userQualifier.getUserFullName(row.createdById) ?: "",
                changedAt = row.requiredCreatedOn()
            )
        }
    }
}
