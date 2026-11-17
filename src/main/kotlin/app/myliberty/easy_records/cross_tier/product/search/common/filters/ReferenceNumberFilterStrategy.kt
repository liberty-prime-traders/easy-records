package app.myliberty.easy_records.cross_tier.product.search.common.filters

import app.myliberty.easy_records.cross_tier.product.search.common.Aliases
import app.myliberty.easy_records.cross_tier.product.search.common.ParameterNames
import app.myliberty.easy_records.util.queries.FilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext

class ReferenceNumberFilterStrategy(private val referenceNumber: String?) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    referenceNumber?.takeIf { it.isNotBlank() }?.let { ref ->
      val p = Aliases.ColumnNames.CrossTierProduct
      context.whereClauses.add("${p.TABLE_ALIAS}.${p.REFERENCE_NUMBER} LIKE :${ParameterNames.REFERENCE_NUMBER}")
      context.params[ParameterNames.REFERENCE_NUMBER] = "$ref%"
    }
  }
}
