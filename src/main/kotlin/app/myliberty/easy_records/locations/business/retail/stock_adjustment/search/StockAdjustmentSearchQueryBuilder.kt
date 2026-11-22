package app.myliberty.easy_records.locations.business.retail.stock_adjustment.search

import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentSearchParameters
import app.myliberty.easy_records.util.model.TableNames
import app.myliberty.easy_records.util.queries.AnyListFilterStrategy
import app.myliberty.easy_records.util.queries.DateRangeFilterStrategy
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import app.myliberty.easy_records.util.queries.QueryBuilderContext
import app.myliberty.easy_records.util.queries.QueryParameterNames
import app.myliberty.easy_records.util.queries.SqlQuery

object StockAdjustmentSearchQueryBuilder {

    fun buildPredicate(stockAdjustmentSearchParameters: StockAdjustmentSearchParameters): QueryBuilderContext {
        val context = QueryBuilderContext()
        context.whereClauses.add("1=1")

        DateRangeFilterStrategy(
            "${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.created_on",
            StockAdjustmentSearchParameterNames.ADJUSTED_FROM,
            stockAdjustmentSearchParameters.adjustedFrom,
            StockAdjustmentSearchParameterNames.ADJUSTED_BEFORE,
            stockAdjustmentSearchParameters.adjustedBefore
        ).apply(context)

        AnyListFilterStrategy(
            "${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.location_product_id",
            StockAdjustmentSearchParameterNames.LOCATION_PRODUCT_IDS,
            stockAdjustmentSearchParameters.locationProductIds.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.category",
            StockAdjustmentSearchParameterNames.CATEGORIES,
            stockAdjustmentSearchParameters.categories.map { category -> category.code }.toTypedArray()
        ).apply(context)

        AnyListFilterStrategy(
            "${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.created_by_id",
            StockAdjustmentSearchParameterNames.ADJUSTED_BY_IDS,
            stockAdjustmentSearchParameters.adjustedByIds.toTypedArray()
        ).apply(context)

        return context
    }

    fun buildListQuery(predicate: QueryBuilderContext, cursor: KeysetSearchCursor?): SqlQuery {
        val whereClauses = predicate.whereClauses.toMutableList()
        val params = predicate.params.toMutableMap()

        if (cursor != null) {
            whereClauses.add(
                "(${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.created_on, " +
                    "${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.id) < " +
                    "(:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"
            )
            params[QueryParameterNames.CURSOR_CREATED_ON] = cursor.createdOn
            params[QueryParameterNames.CURSOR_ID] = cursor.id
        }

        val sql = """
            SELECT ${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.*
            FROM ${TableNames.STOCK_ADJUSTMENT} ${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}
            WHERE ${whereClauses.joinToString(" AND ")}
            ORDER BY ${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.created_on DESC,
                ${StockAdjustmentSearchAliases.STOCK_ADJUSTMENT}.id DESC
            LIMIT :${QueryParameterNames.PAGE_SIZE}
        """.trimIndent()

        return SqlQuery(sql, params, StockAdjustmentSearchQueryMetadata("stock_adjustment_search"))
    }
}
