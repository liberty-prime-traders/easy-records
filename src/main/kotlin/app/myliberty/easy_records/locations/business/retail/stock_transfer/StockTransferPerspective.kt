package app.myliberty.easy_records.locations.business.stock_transfer

import app.myliberty.easy_records.util.enums.HasCode

enum class StockTransferPerspective(override val code: String) : HasCode {
    OUTGOING("OUT"),
    INCOMING("IN")
}
