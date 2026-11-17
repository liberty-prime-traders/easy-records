package app.myliberty.easy_records.organizations.business.org_expense.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.cross_tier.expense.response.ExpenseSummaryResponse
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchFetcher
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchOperationsFactory
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchParameters
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchSummaryResponseDto
import app.myliberty.easy_records.cross_tier.expense.search.ExpenseSearchTables
import app.myliberty.easy_records.organizations.business.org_expense.search.OrgExpenseSearchExecutor
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrgExpenseSearchService(
    expenseSearchOperationsFactory: ExpenseSearchOperationsFactory,
    orgExpenseStore: OrgExpenseStore,
    orgExpenseSearchExecutor: OrgExpenseSearchExecutor
) {

    private val expenseSearchOperations = expenseSearchOperationsFactory.operationsFor(
        orgExpenseStore, ExpenseSearchFetcher(orgExpenseSearchExecutor, ExpenseSearchTables.ORGANIZATION)
    )

    fun search(pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        expenseSearchOperations.search(pageRequest)

    fun summarize(expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        expenseSearchOperations.summarize(expenseSearchParameters)
}
