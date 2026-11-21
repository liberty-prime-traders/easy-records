package app.myliberty.easy_records.cross_tier.retail.product.search.common.filters

import app.myliberty.easy_records.cross_tier.retail.product.search.common.Aliases.ColumnNames.CrossTierProduct
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ParameterNames.PREVIOUS_NAME
import app.myliberty.easy_records.util.queries.FilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext

class NameFilterStrategy(private val previousName: String) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (previousName.isNotBlank()) {
      context.whereClauses.add("LOWER(${CrossTierProduct.TABLE_ALIAS}.${CrossTierProduct.PRODUCT_NAME}) > LOWER(:$PREVIOUS_NAME)")
      context.params[PREVIOUS_NAME] = previousName
    }
  }
}
