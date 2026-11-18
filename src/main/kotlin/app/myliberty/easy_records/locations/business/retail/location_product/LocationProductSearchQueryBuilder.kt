package app.myliberty.easy_records.locations.business.retail.location_product

import app.myliberty.easy_records.cross_tier.retail.product.search.common.Aliases
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.CategoryFilterStrategy
import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.ExcludeIdsFilterStrategy
import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.NameFilterStrategy
import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.ReferenceNumberFilterStrategy
import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.StatusFilterStrategy
import app.myliberty.easy_records.cross_tier.retail.product.search.common.filters.TextSearchFilterStrategy
import app.myliberty.easy_records.util.model.TableNames
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import app.myliberty.easy_records.util.queries.ProductQueryMetadata
import app.myliberty.easy_records.util.queries.QueryParameterNames
import app.myliberty.easy_records.util.queries.SqlQuery

object LocationProductSearchQueryBuilder {

  private val P = Aliases.ColumnNames.CrossTierProduct

  fun buildSearchQuery(searchParams: ProductSearchParameters, previousName: String): SqlQuery {
    val context = QueryBuilderContext()

    context.whereClauses.add("1=1")

    applyFilters(context, searchParams, previousName)

    val sql = buildQuery(context)
    val metadata = buildMetadata(searchParams)

    return SqlQuery(sql, context.params, metadata)
  }

  private fun applyFilters(context: QueryBuilderContext, searchParams: ProductSearchParameters, previousName: String) {
    NameFilterStrategy(previousName).apply(context)
    StatusFilterStrategy(searchParams.extractStatusCodes()).apply(context)
    SalePriceFilterStrategy(searchParams.requireDefaultSalePrice).apply(context)
    ReferenceNumberFilterStrategy(searchParams.referenceNumber).apply(context)
    TextSearchFilterStrategy(searchParams.searchText, searchParams.searchStrategy).apply(context)
    CategoryFilterStrategy(searchParams.categoryIds, P.TABLE_ALIAS).apply(context)
    ExcludeIdsFilterStrategy(searchParams.excludeIds, P.TABLE_ALIAS).apply(context)
  }

  private fun buildQuery(context: QueryBuilderContext): String {
    return """
      SELECT ${P.TABLE_ALIAS}.*
      FROM ${TableNames.LOCATION_PRODUCT} ${P.TABLE_ALIAS}
      WHERE ${context.whereClauses.joinToString(" AND ")}
      ORDER BY LOWER(${P.TABLE_ALIAS}.${P.PRODUCT_NAME})
      LIMIT :${QueryParameterNames.PAGE_SIZE}
    """.trimIndent()
  }

  private fun buildMetadata(searchParams: ProductSearchParameters): ProductQueryMetadata {
    return ProductQueryMetadata(
      queryName = "location_product_search",
      categoryIdsCount = searchParams.categoryIds.size,
      statusListCount = searchParams.extractStatusCodes().size,
      hasTextSearch = !searchParams.searchText.isNullOrBlank()
    )
  }
}
