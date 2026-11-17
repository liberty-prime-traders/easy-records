package app.myliberty.easy_records.locations.business.tax_entry.api

import app.myliberty.easy_records.util.enums.HasCode

enum class TaxSourceType(override val code: String) : HasCode {
    SALE("SL"),
    SALE_VOID("SLV"),
    PURCHASE_DELIVERY("PDL")
}
