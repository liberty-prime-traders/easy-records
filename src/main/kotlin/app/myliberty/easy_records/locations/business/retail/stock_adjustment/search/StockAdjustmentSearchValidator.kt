package app.myliberty.easy_records.locations.business.retail.stock_adjustment.search

import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentSearchParameters
import app.myliberty.easy_records.util.queries.SearchGuards

object StockAdjustmentSearchValidator {

    private const val MAX_LOCATION_PRODUCT_IDS = 100
    private const val MAX_ADJUSTED_BY_IDS = 100
    private const val MIN_PAGE_SIZE = 1
    private const val MAX_PAGE_SIZE = 200

    fun guardValidParameters(stockAdjustmentSearchParameters: StockAdjustmentSearchParameters) {
        SearchGuards.guardBoundedRange(
            stockAdjustmentSearchParameters.adjustedFrom,
            stockAdjustmentSearchParameters.adjustedBefore,
            "adjustedFrom",
            "adjustedBefore"
        )
        SearchGuards.guardRangeOrder(
            stockAdjustmentSearchParameters.adjustedFrom,
            stockAdjustmentSearchParameters.adjustedBefore,
            "adjustedFrom must not be after adjustedBefore"
        ) { from, before -> from.isAfter(before) }
        SearchGuards.guardMaxSize(
            stockAdjustmentSearchParameters.locationProductIds.size,
            MAX_LOCATION_PRODUCT_IDS,
            "location product ids"
        )
        SearchGuards.guardMaxSize(
            stockAdjustmentSearchParameters.adjustedByIds.size,
            MAX_ADJUSTED_BY_IDS,
            "adjusted by ids"
        )
    }

    fun guardValidPageSize(requestedSize: Int) {
        SearchGuards.guardPageSize(requestedSize, MIN_PAGE_SIZE, MAX_PAGE_SIZE)
    }
}
