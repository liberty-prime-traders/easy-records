package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementCapitalRequest
import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementDrawRequest
import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementResponse
import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementSearchParameters
import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementSearchService
import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementService
import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementTransferRequest
import app.myliberty.easy_records.organizations.business.fund_movement.api.FundMovementVoidRequest
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/fund-movements")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class FundMovementEndpoint(
    private val fundMovementService: FundMovementService,
    private val fundMovementSearchService: FundMovementSearchService
) {

    @PostMapping("transfer")
    fun createTransfer(@RequestBody fundMovementTransferRequest: FundMovementTransferRequest): FundMovementResponse =
        fundMovementService.createTransfer(fundMovementTransferRequest)

    @PostMapping("capital")
    fun createCapital(@RequestBody fundMovementCapitalRequest: FundMovementCapitalRequest): FundMovementResponse =
        fundMovementService.createCapital(fundMovementCapitalRequest)

    @PostMapping("draw")
    fun createDraw(@RequestBody fundMovementDrawRequest: FundMovementDrawRequest): FundMovementResponse =
        fundMovementService.createDraw(fundMovementDrawRequest)

    @PostMapping("void")
    fun voidFundMovement(@RequestBody fundMovementVoidRequest: FundMovementVoidRequest): FundMovementResponse =
        fundMovementService.voidFundMovement(fundMovementVoidRequest)

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<FundMovementSearchParameters, String>): PageResponse<FundMovementResponse, String> =
        fundMovementSearchService.search(pageRequest)
}
