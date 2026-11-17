package app.myliberty.easy_records.cross_tier.core.expense.operation

import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseAggregate
import app.myliberty.easy_records.cross_tier.core.expense.model.ExpenseDto
import app.myliberty.easy_records.cross_tier.core.expense.store.ExpenseStore
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.util.UUID

class ExpenseLookup(
    private val expenseStore: ExpenseStore
) {

    fun requireExpense(expenseReference: String): ExpenseDto =
        expenseStore.findExpenseByReference(expenseReference) ?: throw LibertyGenericException("Expense $expenseReference not found")

    fun requireExpenses(expenseReferences: Collection<String>): Map<String, ExpenseDto> {
        val expenseDtosByReference = expenseStore.findExpensesByReferences(expenseReferences).associateBy { it.referenceNumber }
        expenseReferences.firstOrNull { it !in expenseDtosByReference }
            ?.let { throw LibertyGenericException("Expense $it not found") }
        return expenseDtosByReference
    }

    fun requireExpenseById(expenseId: UUID): ExpenseDto =
        expenseStore.findExpenseById(expenseId) ?: throw LibertyGenericException("Expense $expenseId not found")

    fun requireNotVoided(expenseAggregate: ExpenseAggregate, expenseDto: ExpenseDto) {
        if (expenseAggregate.isVoided(expenseDto.id)) {
            throw LibertyGenericException("Expense ${expenseDto.referenceNumber} has been voided")
        }
    }
}
