package app.myliberty.easy_records.locations.business.location_product

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.cross_tier.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.cross_tier.product.search.common.ProductSearchService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema(readOnly = true)
class LocationProductPagedSearch(
    private val locationProductCache: LocationProductCache,
    locationProductDtoFetcher: LocationProductDtoFetcher,
    @Value("\${rts.product-search.client-side-filter-threshold}") clientSideFilterThreshold: Int,
) : ProductSearchService<LocationProductDto>(
    locationProductDtoFetcher,
    LocationProductSearchQueryBuilder::buildSearchQuery,
    clientSideFilterThreshold
) {

    override fun countAllProducts(): Long = locationProductCache.countAllLocationProducts()

    override fun findAllProducts(parameters: ProductSearchParameters): List<LocationProductDto> =
        locationProductCache.findAllLocationProducts().filter { locationProductDto ->
            locationProductDto.status in parameters.statusList &&
                SalePriceFilterStrategy.matches(locationProductDto, parameters.requireDefaultSalePrice)
        }
}
