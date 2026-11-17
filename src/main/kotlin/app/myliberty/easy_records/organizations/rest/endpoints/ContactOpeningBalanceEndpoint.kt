package app.myliberty.easy_records.organizations.rest.endpoints

import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactOpeningBalanceRevisionDto
import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactOpeningBalanceService
import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactOpeningBalanceSetDto
import app.myliberty.easy_records.platform.business.feature.api.Feature
import app.myliberty.easy_records.util.annotations.RequiresFeature
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/contact-opening-balances")
@RequiresFeature(Feature.CHART_OF_ACCOUNTS)
class ContactOpeningBalanceEndpoint(
    private val contactOpeningBalanceService: ContactOpeningBalanceService
) {

    @PostMapping("{contactId}")
    fun set(
        @PathVariable contactId: UUID,
        @RequestBody contactOpeningBalanceSetDto: ContactOpeningBalanceSetDto
    ) = contactOpeningBalanceService.set(contactId, contactOpeningBalanceSetDto)

    @GetMapping("history")
    fun getHistory(@RequestParam contactReferenceNumber: String): List<ContactOpeningBalanceRevisionDto> =
        contactOpeningBalanceService.getHistory(contactReferenceNumber)
}
