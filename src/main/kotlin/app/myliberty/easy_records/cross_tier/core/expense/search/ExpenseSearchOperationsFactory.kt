package app.myliberty.easy_records.cross_tier.core.expense.search

import app.myliberty.easy_records.cross_tier.core.expense.response.ExpenseResponseBuilder
import app.myliberty.easy_records.cross_tier.core.expense.store.ExpenseStore
import app.myliberty.easy_records.organizations.business.core.expense_type.api.ExpenseTypeService
import org.springframework.stereotype.Component

@Component
class ExpenseSearchOperationsFactory(
    private val expenseTypeService: ExpenseTypeService,
    private val expenseResponseBuilder: ExpenseResponseBuilder
) {

    fun operationsFor(expenseStore: ExpenseStore, expenseSearchFetcher: ExpenseSearchFetcher) =
        ExpenseSearchOperations(expenseSearchFetcher, expenseStore, expenseTypeService, expenseResponseBuilder)
}
