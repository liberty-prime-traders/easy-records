package app.myliberty.easy_records.locations.rest.endpoints.retail

import app.myliberty.easy_records.locations.business.retail.reports.payments.api.PaymentRowDto
import app.myliberty.easy_records.locations.business.retail.reports.payments.api.PaymentSearchParameters
import app.myliberty.easy_records.locations.business.retail.reports.payments.api.PaymentSearchService
import app.myliberty.easy_records.locations.business.retail.reports.payments.api.PaymentSummaryResponseDto
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/payments")
class PaymentsReportEndpoint(
    private val paymentSearchService: PaymentSearchService
) {

    @PostMapping("search")
    fun search(
        @RequestBody pageRequest: PageRequest<PaymentSearchParameters, String>
    ): PageResponse<PaymentRowDto, String> {
        return paymentSearchService.search(pageRequest)
    }

    @PostMapping("summary")
    fun searchSummary(@RequestBody paymentSearchParameters: PaymentSearchParameters): PaymentSummaryResponseDto {
        return paymentSearchService.summarize(paymentSearchParameters)
    }
}
