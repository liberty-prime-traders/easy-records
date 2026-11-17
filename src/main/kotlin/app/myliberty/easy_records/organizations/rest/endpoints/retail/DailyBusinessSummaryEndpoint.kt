package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.retail.daily_business_summary.api.DailyBalancesResponse
import app.myliberty.easy_records.organizations.business.retail.daily_business_summary.api.DailyBalancesService
import app.myliberty.easy_records.organizations.business.retail.daily_business_summary.api.DailyBusinessSummaryRequest
import app.myliberty.easy_records.organizations.business.retail.daily_business_summary.api.DailyPerformanceResponse
import app.myliberty.easy_records.organizations.business.retail.daily_business_summary.api.DailyPerformanceService
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/daily-business-summary")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class DailyBusinessSummaryEndpoint(
    private val dailyPerformanceService: DailyPerformanceService,
    private val dailyBalancesService: DailyBalancesService
) {

    @PostMapping("performance")
    fun getPerformance(@RequestBody dailyBusinessSummaryRequest: DailyBusinessSummaryRequest): DailyPerformanceResponse =
        dailyPerformanceService.getPerformance(dailyBusinessSummaryRequest)

    @PostMapping("balances")
    fun getBalances(@RequestBody dailyBusinessSummaryRequest: DailyBusinessSummaryRequest): DailyBalancesResponse =
        dailyBalancesService.getBalances(dailyBusinessSummaryRequest)
}
