package app.myliberty.easy_records.locations.rest.endpoints

import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesLineDto
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesLinesParameters
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesReportSearchMetadata
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesReportSearchParameters
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesReportSearchService
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesReportService
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesSummaryDto
import app.myliberty.easy_records.locations.business.retail.reports.product_sales.api.ProductSalesSummaryParameters
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import app.myliberty.easy_records.util.paging.PageResponseWithMetadata
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/sales/product-report")
class ProductSalesReportEndpoint(
    private val productSalesReportService: ProductSalesReportService,
    private val productSalesReportSearchService: ProductSalesReportSearchService,
) {

    @PostMapping("{locationProductId}/lines")
    fun lines(
        @PathVariable locationProductId: UUID,
        @RequestBody pageRequest: PageRequest<ProductSalesLinesParameters, String>
    ): PageResponse<ProductSalesLineDto, String> {
        return productSalesReportService.searchLines(locationProductId, pageRequest)
    }

    @PostMapping("summary")
    fun summary(
        @RequestBody productSalesSummaryParameters: ProductSalesSummaryParameters
    ): List<ProductSalesSummaryDto> {
        return productSalesReportService.summarize(productSalesSummaryParameters)
    }

    @PostMapping("search")
    fun search(
        @RequestBody pageRequest: PageRequest<ProductSalesReportSearchParameters, String>
    ): PageResponseWithMetadata<ProductSalesSummaryDto, String, ProductSalesReportSearchMetadata> {
        return productSalesReportSearchService.search(pageRequest)
    }
}
