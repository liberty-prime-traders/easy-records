package app.myliberty.easy_records.organizations.business.core.opening_balance

import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.util.business.PositiveMoney
import java.math.BigDecimal
import java.time.LocalDate

object OpeningBalancePostingRequests {

    fun revised(
        ledgerSourceReferenceNumber: String,
        accountCode: String,
        normalBalanceEntryType: EntryType,
        signedDelta: BigDecimal,
        postingDate: LocalDate
    ): LedgerPostingRequest {
        val positiveAmount = PositiveMoney.of(signedDelta.abs())
        val accountEntryType = entryTypeFor(normalBalanceEntryType, signedDelta)
        return LedgerPostingRequest(
            sourceReferenceNumber = ledgerSourceReferenceNumber,
            sourceType = LedgerSourceType.OPENING_BALANCE,
            postingDate = postingDate,
            entries = listOf(
                LedgerEntryRequest(accountCode, accountEntryType, positiveAmount),
                LedgerEntryRequest(SystemAccount.OPENING_BALANCE_EQUITY.code, accountEntryType.opposite(), positiveAmount)
            ),
            subledgerEntries = emptyList()
        )
    }

    private fun entryTypeFor(normalBalanceEntryType: EntryType, signedDelta: BigDecimal): EntryType {
        val isCorrectionReducingBalance = signedDelta.signum() < 0
        return if (isCorrectionReducingBalance) normalBalanceEntryType.opposite() else normalBalanceEntryType
    }
}
