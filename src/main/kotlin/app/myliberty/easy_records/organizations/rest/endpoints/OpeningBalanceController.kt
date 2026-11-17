package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.opening_balance.api.OpeningBalanceRevisionDto
import app.myliberty.easy_records.organizations.business.opening_balance.api.OpeningBalanceService
import app.myliberty.easy_records.organizations.business.opening_balance.api.OpeningBalanceUpsertDto
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/opening-balances")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class OpeningBalanceController(
    private val openingBalanceService: OpeningBalanceService
) {

    @PostMapping
    fun upsert(@RequestBody openingBalanceUpsertDto: OpeningBalanceUpsertDto) =
        openingBalanceService.upsert(openingBalanceUpsertDto)

    @GetMapping("history")
    fun getHistory(@RequestParam accountCode: String): List<OpeningBalanceRevisionDto> =
        openingBalanceService.getHistory(accountCode)
}
