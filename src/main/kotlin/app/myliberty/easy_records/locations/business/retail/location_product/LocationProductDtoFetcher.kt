package app.myliberty.easy_records.locations.business.location_product

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.cross_tier.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import app.myliberty.easy_records.util.queries.FetchesUsingSmartTextStrategy
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductDtoFetcher(
    private val locationProductDtoSearchExecutor: LocationProductDtoSearchExecutor,
) : FetchesUsingSmartTextStrategy<ProductSearchParameters, LocationProductDto> {

    override fun fetch(
        pageRequest: PageRequest<ProductSearchParameters, String>,
        setTimeout: Boolean,
    ): PageResponse<LocationProductDto, String> {
        val sqlQuery = LocationProductSearchQueryBuilder.buildSearchQuery(
            pageRequest.parameters,
            pageRequest.previousCursor,
        )
        val results = locationProductDtoSearchExecutor.execute(sqlQuery, pageRequest.requestedSize + 1, setTimeout)

        val hasMore = results.size > pageRequest.requestedSize
        val pageResults = if (hasMore) results.take(pageRequest.requestedSize) else results
        val currentCursor = pageResults.lastOrNull()?.productName ?: pageRequest.previousCursor

        return PageResponse(
            currentCursor = currentCursor,
            hasMore = hasMore,
            contents = pageResults,
        )
    }
}
