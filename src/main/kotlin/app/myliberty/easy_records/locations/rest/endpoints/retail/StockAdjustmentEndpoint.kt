package app.myliberty.easy_records.locations.rest.endpoints.retail

import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCategoryDescriptor
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCreateDto
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentResponseDto
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentSearchParameters
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentSearchService
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentService
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/stock-adjustments")
class StockAdjustmentEndpoint(
    private val stockAdjustmentService: StockAdjustmentService,
    private val stockAdjustmentSearchService: StockAdjustmentSearchService
) {

    @PostMapping
    fun adjust(@RequestBody stockAdjustmentCreateDto: StockAdjustmentCreateDto): StockAdjustmentResponseDto =
        stockAdjustmentService.adjust(stockAdjustmentCreateDto)

    @GetMapping("categories")
    fun getCategoryDescriptors(): List<StockAdjustmentCategoryDescriptor> =
        stockAdjustmentService.getCategoryDescriptors()

    @PostMapping("search")
    fun search(
        @RequestBody pageRequest: PageRequest<StockAdjustmentSearchParameters, String>
    ): PageResponse<StockAdjustmentResponseDto, String> =
        stockAdjustmentSearchService.search(pageRequest)
}
