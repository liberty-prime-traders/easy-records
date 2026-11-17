package app.myliberty.easy_records.organizations.business.core.opening_balance

import app.myliberty.easy_records.organizations.business.core.account.api.AccountLookupDto
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.util.exceptions.LibertyGenericException

object OpeningBalanceAccountValidator {

    private val manualOpeningBalanceRejections = mapOf(
        SystemAccount.TRADE_RECEIVABLES.code to "is driven by contact opening balances",
        SystemAccount.TRADE_PAYABLES.code to "is driven by contact opening balances",
        SystemAccount.LOANS_PAYABLE.code to "is driven only by loans"
    )

    fun assertManualOpeningBalanceAccepted(accountCode: String) {
        val rejectionReason = manualOpeningBalanceRejections[accountCode] ?: return
        throw LibertyGenericException("Account $accountCode $rejectionReason and cannot take a manual opening balance")
    }

    fun requireLeafActive(account: AccountLookupDto) {
        if (!account.accountIsActive) {
            throw LibertyGenericException("Account ${account.code} is not active")
        }
        if (account.hasChildren) {
            throw LibertyGenericException("Only leaf (postable) accounts can have an opening balance")
        }
    }
}
