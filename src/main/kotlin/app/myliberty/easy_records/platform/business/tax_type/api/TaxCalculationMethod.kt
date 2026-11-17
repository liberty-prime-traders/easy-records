package app.myliberty.easy_records.platform.business.tax_type.api

import app.myliberty.easy_records.util.enums.HasCode

enum class TaxCalculationMethod(override val code: String) : HasCode {
    PERCENTAGE("PCT"),
    FIXED_VALUE("FXD")
}
