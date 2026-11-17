package app.myliberty.easy_records.cross_tier.product.search.common.filters

import app.myliberty.easy_records.cross_tier.product.search.common.Aliases.ColumnNames.CrossTierProduct
import app.myliberty.easy_records.cross_tier.product.search.common.ParameterNames.CATEGORY_IDS
import app.myliberty.easy_records.util.queries.FilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import java.util.UUID

class CategoryFilterStrategy(
  private val categoryIds: Set<UUID>,
  private val tableAlias: String
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (categoryIds.isNotEmpty()) {
      context.whereClauses.add("$tableAlias.${CrossTierProduct.CATEGORY_ID} = ANY(:$CATEGORY_IDS)")
      context.params[CATEGORY_IDS] = categoryIds.toTypedArray()
    }
  }
}
