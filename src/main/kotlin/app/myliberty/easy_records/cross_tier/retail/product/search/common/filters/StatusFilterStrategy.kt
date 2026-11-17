package app.myliberty.easy_records.cross_tier.retail.product.search.common.filters

import app.myliberty.easy_records.cross_tier.retail.product.search.common.Aliases.ColumnNames.CrossTierProduct
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ParameterNames.STATUS_LIST
import app.myliberty.easy_records.util.queries.FilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext

class StatusFilterStrategy(private val statusCodes: Set<String>) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    if (statusCodes.isNotEmpty()) {
      context.whereClauses.add("${CrossTierProduct.TABLE_ALIAS}.${CrossTierProduct.STATUS} = ANY(:$STATUS_LIST)")
      context.params[STATUS_LIST] = statusCodes.toTypedArray()
    }
  }
}
