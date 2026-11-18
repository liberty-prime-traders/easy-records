package app.myliberty.easy_records.locations.rest.endpoints.retail

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleNotesUpdateDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSearchParameters
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSearchService
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSearchSummaryResponseDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSummary
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleUpdater
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/sales")
class SaleEndpoint(
    private val saleDataFetcher: SaleDataFetcher,
    private val saleUpdater: SaleUpdater,
    private val saleSearchService: SaleSearchService,
) {

    @GetMapping
    fun fetchRecent(@RequestParam n: Int?): List<SaleSummary> {
        return saleDataFetcher.fetchRecent(n)
    }

    @PutMapping("{saleId}/notes")
    fun updateNotes(@PathVariable saleId: UUID, @RequestBody dto: SaleNotesUpdateDto): ResponseEntity<Unit> {
        saleUpdater.updateNotes(saleId, dto.notes)
        return ResponseEntity.ok().build()
    }

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<SaleSearchParameters, String>): PageResponse<SaleSummary, String> {
        return saleSearchService.search(pageRequest)
    }

    @PostMapping("search/summary")
    fun searchSummary(@RequestBody saleSearchParameters: SaleSearchParameters): SaleSearchSummaryResponseDto {
        return saleSearchService.summarize(saleSearchParameters)
    }
}
