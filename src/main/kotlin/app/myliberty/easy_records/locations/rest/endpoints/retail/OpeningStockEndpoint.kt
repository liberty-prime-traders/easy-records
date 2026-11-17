package app.myliberty.easy_records.locations.rest.endpoints

import app.myliberty.easy_records.locations.business.retail.opening_stock.api.OpeningStockBulkLineDto
import app.myliberty.easy_records.locations.business.retail.opening_stock.api.OpeningStockBulkService
import app.myliberty.easy_records.locations.business.retail.opening_stock.api.OpeningStockLineDto
import app.myliberty.easy_records.locations.business.retail.opening_stock.api.OpeningStockResponseDto
import app.myliberty.easy_records.locations.business.retail.opening_stock.api.OpeningStockService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("secured/opening-stock")
class OpeningStockEndpoint(
    private val openingStockService: OpeningStockService,
    private val openingStockBulkService: OpeningStockBulkService
) {

    @PostMapping
    fun declareInitialStock(@RequestBody lines: List<OpeningStockLineDto>): List<OpeningStockResponseDto> =
        openingStockService.declareInitialStock(lines)

    @PostMapping("bulk")
    fun bulkDeclareInitialStock(@RequestBody openingStockBulkLineDtos: List<OpeningStockBulkLineDto>): List<OpeningStockResponseDto> =
        openingStockBulkService.bulkDeclareInitialStock(openingStockBulkLineDtos)

    @GetMapping
    fun getAll(): List<OpeningStockResponseDto> =
        openingStockService.getAll()
}
