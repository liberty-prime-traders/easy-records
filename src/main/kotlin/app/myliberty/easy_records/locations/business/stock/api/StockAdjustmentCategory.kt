package app.myliberty.easy_records.locations.business.stock.api

import app.myliberty.easy_records.util.enums.HasCode

enum class StockAdjustmentCategory(override val code: String) : HasCode {
    INTERNAL_USE("IU"),
    OPENING_STOCK("OS"),
    FREE_GOODS("FG"),
    OTHER("OTH")
}
