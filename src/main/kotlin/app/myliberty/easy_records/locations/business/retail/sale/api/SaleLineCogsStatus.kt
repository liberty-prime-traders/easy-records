package app.myliberty.easy_records.locations.business.sale.api

import app.myliberty.easy_records.util.enums.HasCode

enum class SaleLineCogsStatus(override val code: String) : HasCode {
    ACTIVE("ACT"),
    REPLACED("RPL")
}
