package app.myliberty.easy_records.organizations.rest.endpoints.retail

import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductInsertDto
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductResponseDto
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductSearchService
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductService
import app.myliberty.easy_records.organizations.business.retail.product.api.OrganizationProductUpdateDto
import app.myliberty.easy_records.organizations.business.retail.product_bulk_import.api.BulkProductImportRequestDto
import app.myliberty.easy_records.organizations.business.retail.product_bulk_import.api.BulkProductImportService
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("secured/products")
class ProductEndpoint(
    private val organizationProductService: OrganizationProductService,
    private val organizationProductSearchService: OrganizationProductSearchService,
    private val bulkProductImportService: BulkProductImportService
) {

    @PostMapping
    fun createProduct(@RequestBody productInsertDto: OrganizationProductInsertDto): OrganizationProductResponseDto =
        organizationProductService.createProduct(productInsertDto)

    @PostMapping("bulk")
    @ResponseStatus(HttpStatus.OK)
    fun bulkImportProducts(@RequestBody bulkProductImportRequestDto: BulkProductImportRequestDto) {
        bulkProductImportService.bulkImport(bulkProductImportRequestDto)
    }

    @PostMapping("search")
    fun search(@RequestBody pageRequest: PageRequest<ProductSearchParameters, String>): PageResponse<OrganizationProductResponseDto, String> =
        organizationProductSearchService.searchWithParameters(pageRequest)

    @PostMapping("search/debug-query")
    fun debugSearchQuery(@RequestBody pageRequest: PageRequest<ProductSearchParameters, String>): String =
        organizationProductSearchService.generateFormattedQuery(pageRequest)

    @PutMapping
    fun updateProduct(@RequestBody productDto: OrganizationProductUpdateDto): OrganizationProductResponseDto =
        organizationProductService.updateProduct(productDto)

    @PutMapping("{orgProductId}/deactivate")
    fun deactivateProduct(@PathVariable orgProductId: UUID): OrganizationProductResponseDto =
        organizationProductService.deactivateProduct(orgProductId)

    @PutMapping("{orgProductId}/reactivate")
    fun reactivateProduct(@PathVariable orgProductId: UUID): OrganizationProductResponseDto =
        organizationProductService.reactivateProduct(orgProductId)

}
