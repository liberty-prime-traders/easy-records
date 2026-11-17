package app.myliberty.easy_records.locations.business.retail.tax_entry.api

import app.myliberty.easy_records.util.enums.HasCode

enum class TaxDirection(override val code: String) : HasCode {
    OUTPUT("O"),
    INPUT("I")
}
