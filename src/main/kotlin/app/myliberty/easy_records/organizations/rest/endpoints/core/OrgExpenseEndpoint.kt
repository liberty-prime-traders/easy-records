package app.myliberty.easy_records.organizations.rest.endpoints.core

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpensePaymentCreateRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpensePaymentVoidRequest
import app.myliberty.easy_records.cross_tier.core.expense.response.ExpenseSummaryResponse
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpenseVoidRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.StandaloneExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.StockTransferExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.core.expense.search.ExpenseSearchParameters
import app.myliberty.easy_records.cross_tier.core.expense.search.ExpenseSearchSummaryResponseDto
import app.myliberty.easy_records.organizations.business.core.org_expense.api.OrgExpenseSearchService
import app.myliberty.easy_records.organizations.business.core.org_expense.api.OrgExpenseService
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/org-expenses")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class OrgExpenseEndpoint(
    private val orgExpenseService: OrgExpenseService,
    private val orgExpenseSearchService: OrgExpenseSearchService
) {

    @PostMapping("standalone")
    fun createStandalone(@RequestBody standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        orgExpenseService.createStandalone(standaloneExpenseBatchRequest)

    @PostMapping("stock-transfer")
    fun createForStockTransfer(
        @RequestBody stockTransferExpenseBatchRequest: StockTransferExpenseBatchRequest
    ): List<ExpenseSummaryResponse> = orgExpenseService.createForStockTransfer(stockTransferExpenseBatchRequest)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        orgExpenseSearchService.search(pageRequest)

    @PostMapping("search/summary")
    fun searchSummary(@RequestBody expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        orgExpenseSearchService.summarize(expenseSearchParameters)

    @PostMapping("payments")
    fun recordPayments(@RequestBody expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        orgExpenseService.recordPayments(expensePaymentCreateRequests)

    @PostMapping("payments/void")
    fun voidPayment(@RequestBody expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        orgExpenseService.voidPayment(expensePaymentVoidRequest)

    @PostMapping("void")
    fun voidExpense(@RequestBody expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        orgExpenseService.voidExpense(expenseVoidRequest)

    @GetMapping("by-source")
    fun getBySource(
        @RequestParam sourceType: ExpenseSourceType,
        @RequestParam sourceReference: String
    ): List<ExpenseSummaryResponse> = orgExpenseService.getBySource(sourceType, sourceReference)

    @GetMapping("{expenseReference}")
    fun getExpense(@PathVariable expenseReference: String): ExpenseSummaryResponse =
        orgExpenseService.getExpense(expenseReference)
}
