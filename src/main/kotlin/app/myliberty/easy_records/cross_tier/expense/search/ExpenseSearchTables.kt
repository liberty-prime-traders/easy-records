package app.myliberty.easy_records.cross_tier.expense.search

import app.myliberty.easy_records.util.model.TableNames
import app.myliberty.easy_records.util.model.ViewNames

data class ExpenseSearchTables(
    val expense: String,
    val paymentState: String,
    val expenseVoid: String,
    val activeExpensePayment: String
) {
    companion object {
        val LOCATION = ExpenseSearchTables(
            TableNames.EXPENSE, TableNames.EXPENSE_PAYMENT_STATE, TableNames.EXPENSE_VOID, ViewNames.ACTIVE_EXPENSE_PAYMENT
        )

        val ORGANIZATION = ExpenseSearchTables(
            TableNames.ORG_EXPENSE, TableNames.ORG_EXPENSE_PAYMENT_STATE, TableNames.ORG_EXPENSE_VOID, ViewNames.ACTIVE_ORG_EXPENSE_PAYMENT
        )
    }
}
