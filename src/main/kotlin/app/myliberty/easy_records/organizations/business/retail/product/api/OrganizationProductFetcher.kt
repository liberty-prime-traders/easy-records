package app.myliberty.easy_records.organizations.business.retail.product.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnOrganizationSchema
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.organizations.business.retail.product.OrganizationProductCache
import app.myliberty.easy_records.organizations.business.retail.product.OrganizationProductMapper
import app.myliberty.easy_records.organizations.business.retail.product.OrganizationProductQueryBuilder
import app.myliberty.easy_records.organizations.business.retail.product.OrganizationProductSearchExecutor
import app.myliberty.easy_records.organizations.business.retail.product_tag.api.ProductTagService
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import app.myliberty.easy_records.util.queries.FetchesUsingSmartTextStrategy
import org.springframework.stereotype.Service

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class OrganizationProductFetcher(
    private val executor: OrganizationProductSearchExecutor,
    private val productTagService: ProductTagService,
    private val unitValueFetcher: UnitValueFetcher,
    private val organizationProductCache: OrganizationProductCache,
    private val organizationProductMapper: OrganizationProductMapper
): FetchesUsingSmartTextStrategy<ProductSearchParameters, OrganizationProductResponseDto> {

    override fun fetch(
        pageRequest: PageRequest<ProductSearchParameters, String>,
        setTimeout: Boolean
    ): PageResponse<OrganizationProductResponseDto, String> {
        val sqlQuery = OrganizationProductQueryBuilder.buildSearchQuery(
            pageRequest.parameters,
            pageRequest.previousCursor
        )
        val results = executor.execute(sqlQuery, pageRequest.requestedSize + 1, setTimeout)

        val hasMore = results.size > pageRequest.requestedSize
        val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
        val contents = productTagService.populateTagsForProducts(pageResults)
        val currentCursor = contents.lastOrNull()?.productName ?: pageRequest.previousCursor

        return PageResponse(
            currentCursor = currentCursor,
            hasMore = hasMore,
            contents = contents
        )
    }

    fun findAllProducts(): List<OrganizationProductResponseDto> {
        val unitNamesById = unitValueFetcher.getUnitNamesById()
        val responseDtos = organizationProductCache.findAllProducts().map {
            val baseUnit = unitNamesById[it.baseUnitId]
                ?: throw LibertyGenericException("Unit name not found for id ${it.baseUnitId}")
            organizationProductMapper.toResponseDtoWithoutTags(it, baseUnit)
        }
        return productTagService.populateTagsForProducts(responseDtos)
    }

    fun countAllProducts(): Long = organizationProductCache.countAllProducts()
}
