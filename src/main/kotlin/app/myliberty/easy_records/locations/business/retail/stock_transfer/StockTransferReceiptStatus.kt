package app.myliberty.easy_records.locations.business.retail.stock_transfer

import app.myliberty.easy_records.util.enums.HasCode

enum class StockTransferReceiptStatus(override val code: String) : HasCode {
    PENDING("P"),
    COMPLETED("C")
}
