package app.myliberty.easy_records.locations.rest.endpoints

import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesDeliveryLineDto
import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesDeliveryLinesParameters
import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesReportParameters
import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesReportRowDto
import app.myliberty.easy_records.locations.business.retail.reports.product_purchases.api.ProductPurchasesReportService
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/product-purchases-report")
class ProductPurchasesReportEndpoint(
    private val productPurchasesReportService: ProductPurchasesReportService,
) {

    @PostMapping("summary")
    fun summary(
        @RequestBody productPurchasesReportParameters: ProductPurchasesReportParameters
    ): List<ProductPurchasesReportRowDto> {
        return productPurchasesReportService.summarize(productPurchasesReportParameters)
    }

    @PostMapping("{locationProductId}/delivery-lines")
    fun deliveryLines(
        @PathVariable locationProductId: UUID,
        @RequestBody pageRequest: PageRequest<ProductPurchasesDeliveryLinesParameters, String>
    ): PageResponse<ProductPurchasesDeliveryLineDto, String> {
        return productPurchasesReportService.searchDeliveryLines(locationProductId, pageRequest)
    }
}
