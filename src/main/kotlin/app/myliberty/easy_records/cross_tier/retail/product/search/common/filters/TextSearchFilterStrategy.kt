package app.myliberty.easy_records.cross_tier.retail.product.search.common.filters

import app.myliberty.easy_records.cross_tier.retail.product.search.common.Aliases
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ParameterNames.NAME_OR_DESCRIPTION
import app.myliberty.easy_records.util.queries.FilterStrategy
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import app.myliberty.easy_records.util.queries.SearchStrategy

class TextSearchFilterStrategy(
  private val searchText: String?,
  private val searchStrategy: SearchStrategy
) : FilterStrategy {

  override fun apply(context: QueryBuilderContext) {
    searchText?.takeIf { it.isNotBlank() }?.let { text ->
      if (searchStrategy == SearchStrategy.NONE) return

      val p = Aliases.ColumnNames.CrossTierProduct

      when (searchStrategy) {
        SearchStrategy.FULLTEXT -> {
          context.whereClauses.add(
            "${p.TABLE_ALIAS}.${p.SEARCH_VECTOR} @@ plainto_tsquery('english', :$NAME_OR_DESCRIPTION)"
          )
          context.params[NAME_OR_DESCRIPTION] = text
        }

        SearchStrategy.TRIGRAM -> {
          val conditions = buildString {
            append("LOWER(${p.TABLE_ALIAS}.${p.PRODUCT_NAME}) % LOWER(:$NAME_OR_DESCRIPTION) ")
            append("OR LOWER(${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_NAME}) % LOWER(:$NAME_OR_DESCRIPTION)")
            append(" OR LOWER(${p.TABLE_ALIAS}.${p.DESCRIPTION}) % LOWER(:$NAME_OR_DESCRIPTION)")
          }
          context.whereClauses.add("($conditions)")
          context.params[NAME_OR_DESCRIPTION] = text
        }

        SearchStrategy.PREFIX -> {
          context.whereClauses.add(
            "(LOWER(${p.TABLE_ALIAS}.${p.PRODUCT_NAME}) LIKE LOWER(:$NAME_OR_DESCRIPTION) " +
              "OR LOWER(${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_NAME}) LIKE LOWER(:$NAME_OR_DESCRIPTION))"
          )
          context.params[NAME_OR_DESCRIPTION] = "$text%"
        }

        SearchStrategy.WILDCARD -> {
          val conditions = buildString {
            append("LOWER(${p.TABLE_ALIAS}.${p.PRODUCT_NAME}) LIKE LOWER(:$NAME_OR_DESCRIPTION) ")
            append("OR LOWER(${p.TABLE_ALIAS}.${p.PRODUCT_GROUP_NAME}) LIKE LOWER(:$NAME_OR_DESCRIPTION)")
            append(" OR LOWER(${p.TABLE_ALIAS}.${p.DESCRIPTION}) LIKE LOWER(:$NAME_OR_DESCRIPTION)")
          }
          context.whereClauses.add("($conditions)")
          context.params[NAME_OR_DESCRIPTION] = "%$text%"
        }

        SearchStrategy.NONE -> return
      }
    }
  }
}
