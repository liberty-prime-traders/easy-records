package app.myliberty.easy_records.cross_tier.expense.operation

import app.myliberty.easy_records.cross_tier.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.expense.ExpenseTier
import app.myliberty.easy_records.cross_tier.expense.response.ExpenseResponseBuilder
import app.myliberty.easy_records.cross_tier.expense.response.ExpenseSummaryResponse

class ExpenseReadOperations(
    expenseTier: ExpenseTier,
    private val expenseResponseBuilder: ExpenseResponseBuilder,
    private val expenseLookup: ExpenseLookup
) {

    private val expenseStore = expenseTier.expenseStore

    fun getExpense(expenseReference: String): ExpenseSummaryResponse {
        val expenseDto = expenseLookup.requireExpense(expenseReference)
        val expenseAggregate = expenseStore.loadForExpenses(listOf(expenseDto.id))
        return expenseResponseBuilder.buildSummary(expenseAggregate, expenseDto.id)
    }

    fun getBySource(sourceType: ExpenseSourceType, sourceReference: String): List<ExpenseSummaryResponse> {
        val expenseBatchDto = expenseStore.findBatchBySource(sourceType, sourceReference) ?: return emptyList()
        val expenseAggregate = expenseStore.loadForBatch(expenseBatchDto.id)
        return expenseResponseBuilder.buildSummaries(expenseAggregate).sortedBy { it.createdOn }
    }
}
