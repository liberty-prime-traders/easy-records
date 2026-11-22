package app.myliberty.easy_records.locations.business.retail.stock_adjustment.search

import app.myliberty.easy_records.locations.business.retail.stock_adjustment.StockAdjustmentEntity
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentSearchParameters
import app.myliberty.easy_records.util.queries.KeysetSearchCursor
import org.springframework.stereotype.Component

@Component
class StockAdjustmentSearchFetcher(
    private val stockAdjustmentSearchListExecutor: StockAdjustmentSearchListExecutor
) {

    fun search(
        stockAdjustmentSearchParameters: StockAdjustmentSearchParameters,
        cursor: KeysetSearchCursor?,
        requestedSize: Int
    ): List<StockAdjustmentEntity> {
        val predicate = StockAdjustmentSearchQueryBuilder.buildPredicate(stockAdjustmentSearchParameters)
        val sqlQuery = StockAdjustmentSearchQueryBuilder.buildListQuery(predicate, cursor)
        return stockAdjustmentSearchListExecutor.execute(sqlQuery, requestedSize + 1, setTimeout = true)
    }
}
