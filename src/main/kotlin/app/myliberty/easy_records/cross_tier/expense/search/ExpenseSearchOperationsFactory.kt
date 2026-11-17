package app.myliberty.easy_records.cross_tier.expense.search

import app.myliberty.easy_records.cross_tier.expense.response.ExpenseResponseBuilder
import app.myliberty.easy_records.cross_tier.expense.store.ExpenseStore
import app.myliberty.easy_records.organizations.business.expense_type.api.ExpenseTypeService
import org.springframework.stereotype.Component

@Component
class ExpenseSearchOperationsFactory(
    private val expenseTypeService: ExpenseTypeService,
    private val expenseResponseBuilder: ExpenseResponseBuilder
) {

    fun operationsFor(expenseStore: ExpenseStore, expenseSearchFetcher: ExpenseSearchFetcher) =
        ExpenseSearchOperations(expenseSearchFetcher, expenseStore, expenseTypeService, expenseResponseBuilder)
}
