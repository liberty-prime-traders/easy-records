package app.myliberty.easy_records.locations.rest.endpoints

import app.myliberty.easy_records.cross_tier.core.expense.ExpenseSourceType
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpensePaymentCreateRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpensePaymentVoidRequest
import app.myliberty.easy_records.cross_tier.core.expense.response.ExpenseSummaryResponse
import app.myliberty.easy_records.cross_tier.core.expense.request.ExpenseVoidRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.PurchaseExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.SaleExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.StandaloneExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.core.expense.request.WageExpenseBatchRequest
import app.myliberty.easy_records.cross_tier.core.expense.search.ExpenseSearchParameters
import app.myliberty.easy_records.cross_tier.core.expense.search.ExpenseSearchSummaryResponseDto
import app.myliberty.easy_records.locations.business.core.expense.api.ExpenseSearchService
import app.myliberty.easy_records.locations.business.core.expense.api.ExpenseService
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
@RequestMapping("secured/expenses")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ExpenseEndpoint(
    private val expenseService: ExpenseService,
    private val expenseSearchService: ExpenseSearchService
) {

    @PostMapping("standalone")
    fun createStandalone(@RequestBody standaloneExpenseBatchRequest: StandaloneExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseService.createStandalone(standaloneExpenseBatchRequest)

    @PostMapping("wages")
    fun createWages(@RequestBody wageExpenseBatchRequest: WageExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseService.createWages(wageExpenseBatchRequest)

    @PostMapping("purchase")
    fun createForPurchase(@RequestBody purchaseExpenseBatchRequest: PurchaseExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseService.createForPurchase(purchaseExpenseBatchRequest)

    @PostMapping("sale")
    fun createForSale(@RequestBody saleExpenseBatchRequest: SaleExpenseBatchRequest): List<ExpenseSummaryResponse> =
        expenseService.createForSale(saleExpenseBatchRequest)

    @PostMapping("payments")
    fun recordPayments(@RequestBody expensePaymentCreateRequests: List<ExpensePaymentCreateRequest>): List<ExpenseSummaryResponse> =
        expenseService.recordPayments(expensePaymentCreateRequests)

    @PostMapping("payments/void")
    fun voidPayment(@RequestBody expensePaymentVoidRequest: ExpensePaymentVoidRequest): ExpenseSummaryResponse =
        expenseService.voidPayment(expensePaymentVoidRequest)

    @PostMapping("void")
    fun voidExpense(@RequestBody expenseVoidRequest: ExpenseVoidRequest): ExpenseSummaryResponse =
        expenseService.voidExpense(expenseVoidRequest)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<ExpenseSearchParameters, String>): PageResponse<ExpenseSummaryResponse, String> =
        expenseSearchService.search(pageRequest)

    @PostMapping("search/summary")
    fun searchSummary(@RequestBody expenseSearchParameters: ExpenseSearchParameters): ExpenseSearchSummaryResponseDto =
        expenseSearchService.summarize(expenseSearchParameters)

    @GetMapping("by-source")
    fun getBySource(
        @RequestParam sourceType: ExpenseSourceType,
        @RequestParam sourceReference: String
    ): List<ExpenseSummaryResponse> = expenseService.getBySource(sourceType, sourceReference)

    @GetMapping("{expenseReference}")
    fun getExpense(@PathVariable expenseReference: String): ExpenseSummaryResponse =
        expenseService.getExpense(expenseReference)
}
