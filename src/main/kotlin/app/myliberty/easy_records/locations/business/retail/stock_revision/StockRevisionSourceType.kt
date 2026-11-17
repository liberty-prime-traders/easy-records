package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.util.enums.HasCode

enum class StockRevisionSourceType(override val code: String) : HasCode {
    DELIVERY_LINE("DLL")
}
