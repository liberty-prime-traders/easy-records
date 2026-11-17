package app.myliberty.easy_records.locations.business.expense.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.cross_tier.expense.response.ExpenseSummaryResponse
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchFetcher
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchOperationsFactory
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchParameters
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchSummaryResponseDto
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchTables
import app.myliberty.easy_records.locations.business.expense.ExpenseSearchExecutor
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class ExpenseSearchService(
    expenseSearchOperationsFactory: ExpenseSearchOperationsFactory,
    locationExpenseStore: LocationExpenseStore,
    expenseSearchExecutor: ExpenseSearchExecutor
) {

    private val expenseSearchOperations = expenseSearchOperationsFactory.operationsFor(
        locationExpenseStore, ExpenseSearchFetcher(expenseSearchExecutor, ExpenseSearchTables.LOCATION)
    )

    fun search(pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        expenseSearchOperations.search(pageRequest)

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        expenseSearchOperations.summarize(expenseSearchParameters)
}
