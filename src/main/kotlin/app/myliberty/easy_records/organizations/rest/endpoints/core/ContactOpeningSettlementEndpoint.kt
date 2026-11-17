package app.myliberty.easy_records.organizations.rest.endpoints.core

import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactOpeningSettlementCreateRequest
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactOpeningSettlementService
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactOpeningSettlementSummaryResponse
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactOpeningSettlementVoidRequest
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/contact-opening-settlements")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ContactOpeningSettlementEndpoint(
    private val contactOpeningSettlementService: ContactOpeningSettlementService
) {

    @PostMapping
    fun recordSettlement(
        @RequestBody contactOpeningSettlementCreateRequest: ContactOpeningSettlementCreateRequest
    ): ContactOpeningSettlementSummaryResponse =
        contactOpeningSettlementService.recordSettlement(contactOpeningSettlementCreateRequest)

    @PostMapping("void")
    fun voidSettlement(
        @RequestBody contactOpeningSettlementVoidRequest: ContactOpeningSettlementVoidRequest
    ): ContactOpeningSettlementSummaryResponse =
        contactOpeningSettlementService.voidSettlement(contactOpeningSettlementVoidRequest)

    @GetMapping
    fun getSummary(@RequestParam contactReferenceNumber: String): ContactOpeningSettlementSummaryResponse =
        contactOpeningSettlementService.getSummary(contactReferenceNumber)
}
