package app.myliberty.easy_records.organizations.rest.endpoints.core

import app.myliberty.easy_records.organizations.business.core.loan.api.LoanCreateRequest
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanRepaymentCreateRequest
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanRepaymentService
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanRepaymentVoidRequest
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanResponse
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanSearchParameters
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanSearchService
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanService
import app.myliberty.easy_records.organizations.business.core.loan.api.LoanVoidRequest
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/loans")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class LoanEndpoint(
    private val loanService: LoanService,
    private val loanRepaymentService: LoanRepaymentService,
    private val loanSearchService: LoanSearchService
) {

    @PostMapping
    fun createLoan(@RequestBody loanCreateRequest: LoanCreateRequest): LoanResponse =
        loanService.createLoan(loanCreateRequest)

    @PostMapping("repayments")
    fun recordRepayment(@RequestBody loanRepaymentCreateRequest: LoanRepaymentCreateRequest): LoanResponse =
        loanRepaymentService.recordRepayment(loanRepaymentCreateRequest)

    @PostMapping("void")
    fun voidLoan(@RequestBody loanVoidRequest: LoanVoidRequest): LoanResponse =
        loanService.voidLoan(loanVoidRequest)

    @PostMapping("repayments/void")
    fun voidRepayment(@RequestBody loanRepaymentVoidRequest: LoanRepaymentVoidRequest): LoanResponse =
        loanRepaymentService.voidRepayment(loanRepaymentVoidRequest)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<LoanSearchParameters, String>): PageResponse<LoanResponse, String> =
        loanSearchService.search(pageRequest)

    @GetMapping("{loanReference}")
    fun getLoan(@PathVariable loanReference: String): LoanResponse =
        loanService.getLoan(loanReference)
}
