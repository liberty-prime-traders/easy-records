package app.myliberty.easy_records.locations.rest.endpoints.core

import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxEntrySearchParameters
import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxEntrySearchResultDto
import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxEntrySearchService
import app.myliberty.easy_records.locations.business.core.tax_entry.api.TaxEntrySearchSummaryResponseDto
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/tax-entries")
class TaxEntryEndpoint(
    private val taxEntrySearchService: TaxEntrySearchService
) {

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<TaxEntrySearchParameters, String>): PageResponse<TaxEntrySearchResultDto, String> {
        return taxEntrySearchService.search(pageRequest)
    }

    @PostMapping("search/summary")
    fun searchSummary(@RequestBody taxEntrySearchParameters: TaxEntrySearchParameters): TaxEntrySearchSummaryResponseDto {
        return taxEntrySearchService.summarize(taxEntrySearchParameters)
    }
}
