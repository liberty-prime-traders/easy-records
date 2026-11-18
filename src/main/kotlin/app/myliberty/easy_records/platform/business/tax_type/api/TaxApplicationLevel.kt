package app.myliberty.easy_records.platform.business.tax_type.api

import app.myliberty.easy_records.util.enums.HasCode

enum class TaxApplicationLevel(override val code: String) : HasCode {
    LINE_ITEM("LN"),
    TRANSACTION("TX")
}
