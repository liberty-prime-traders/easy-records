package app.myliberty.easy_records.cross_tier.product.search.common.filters

import app.myliberty.easy_records.cross_tier.product.search.common.Aliases.ColumnNames.CrossTierProduct
import app.myliberty.easy_records.cross_tier.product.search.common.ParameterNames.EXCLUDE_IDS
import app.myliberty.easy_records.util.queries.FilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import java.util.UUID

class ExcludeIdsFilterStrategy(
  private val excludeIds: Set<UUID>,
  private val tableAlias: String
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (excludeIds.isNotEmpty()) {
      context.whereClauses.add("$tableAlias.${CrossTierProduct.ID} != ALL(:$EXCLUDE_IDS)")
      context.params[EXCLUDE_IDS] = excludeIds.toTypedArray()
    }
  }
}
