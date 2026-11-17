package app.myliberty.easy_records.locations.business.location_product

import app.myliberty.easy_records.cross_tier.product.search.common.Aliases.ColumnNames.CrossTierProduct
import app.myliberty.easy_records.util.queries.FilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext

class SalePriceFilterStrategy(private val requireDefaultSalePrice: Boolean) : FilterStrategy {

    override fun apply(context: QueryBuilderContext) {
        if (requireDefaultSalePrice) {
            context.whereClauses.add("${CrossTierProduct.TABLE_ALIAS}.$DEFAULT_SALE_PRICE_COLUMN IS NOT NULL")
        }
    }

    companion object {
        private const val DEFAULT_SALE_PRICE_COLUMN = "default_sale_price"

        fun matches(locationProductDto: LocationProductDto, requireDefaultSalePrice: Boolean): Boolean =
            !requireDefaultSalePrice || locationProductDto.defaultSalePrice != null
    }
}
