package app.myliberty.easy_records.organizations.business.ledger.helpers

import app.myliberty.easy_records.organizations.business.account.api.SystemAccount

object ExpenseLiabilityAccount {

    fun forExpenseAccount(expenseAccountCode: String): String =
        if (expenseAccountCode == SystemAccount.WAGES_EXPENSE.code) {
            SystemAccount.WAGES_PAYABLE.code
        } else {
            SystemAccount.TRADE_PAYABLES.code
        }
}
