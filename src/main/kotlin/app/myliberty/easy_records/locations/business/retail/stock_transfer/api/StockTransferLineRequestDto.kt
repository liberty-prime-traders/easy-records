package app.myliberty.easy_records.locations.business.stock_transfer.api

data class StockTransferLineRequestDto(
    val additions: List<StockTransferLineInsertDto> = emptyList(),
    val updates: List<StockTransferLineUpdateDto> = emptyList()
)
