package app.myliberty.easy_records.locations.business.stock.api

import app.myliberty.easy_records.util.enums.HasCode

enum class StockEntryStatus(override val code: String) : HasCode {
    ACTIVE("ACT"),
    REPLACED("RPL")
}
