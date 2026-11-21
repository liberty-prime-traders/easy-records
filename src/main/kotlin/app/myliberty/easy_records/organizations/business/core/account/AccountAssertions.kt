package app.myliberty.easy_records.organizations.business.core.account

import app.myliberty.easy_records.organizations.business.core.account.api.AccountPosting
import app.myliberty.easy_records.util.exceptions.LibertyGenericException

object AccountAssertions {

    fun assertPostable(
        accounts: Collection<AccountDto>,
        accountPostings: List<AccountPosting>,
        inactiveAccountsMayIncrease: Boolean = false
    ) {
        val accountsByCode = accounts.associateBy { it.code }
        val parentAccountCodes = parentAccountCodesOf(accounts)
        accountPostings.forEach { accountPosting ->
            val account = requireKnown(accountsByCode, accountPosting.accountCode)
            requireLeaf(account, parentAccountCodes)
            // An inactive account may still be drawn down (reversals, settling old balances), never built up.
            val increasesBalance = accountPosting.entryType == account.accountType.normalBalance
            if (account.accountIsActive.not() && increasesBalance && !inactiveAccountsMayIncrease) {
                throw LibertyGenericException("Cannot post an increase to inactive account ${account.label}")
            }
        }
    }

    private fun parentAccountCodesOf(accounts: Collection<AccountDto>): Set<String> =
        accounts.mapNotNull { it.parentAccountCode }.toSet()

    private fun requireKnown(accountsByCode: Map<String, AccountDto>, accountCode: String): AccountDto =
        accountsByCode[accountCode] ?: throw LibertyGenericException("Cannot post to unknown account $accountCode")

    private fun requireLeaf(account: AccountDto, parentAccountCodes: Set<String>) {
        if (account.code in parentAccountCodes) {
            throw LibertyGenericException("Cannot post to ${account.label}: accounts with children never post directly")
        }
    }
}
