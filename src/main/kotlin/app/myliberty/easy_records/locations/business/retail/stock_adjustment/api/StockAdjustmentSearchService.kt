package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.locations.business.retail.stock_adjustment.search.StockAdjustmentSearchValidator
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.paging.PageResponse
import org.springframework.stereotype.Service

@Service
class StockAdjustmentSearchService(
    private val stockAdjustmentDataFetcher: StockAdjustmentDataFetcher
) {

    fun search(
        pageRequest: PageRequest<StockAdjustmentSearchParameters, String>
    ): PageResponse<StockAdjustmentResponseDto, String> {
        StockAdjustmentSearchValidator.guardValidParameters(pageRequest.parameters)
        StockAdjustmentSearchValidator.guardValidPageSize(pageRequest.requestedSize)

        return stockAdjustmentDataFetcher.search(
            pageRequest.parameters, pageRequest.previousCursor, pageRequest.requestedSize
        )
    }
}
