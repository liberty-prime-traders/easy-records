package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import app.myliberty.easy_records.util.enums.HasCode

enum class StockAdjustmentCostStatus(override val code: String) : HasCode {
    ACTIVE("ACT"),
    REPLACED("RPL")
}
