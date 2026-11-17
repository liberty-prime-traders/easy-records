package app.myliberty.easy_records.locations.business.retail.sale.api

import app.myliberty.easy_records.util.enums.HasCode

enum class SaleStatus(override val code: String) : HasCode {
    DRAFT("DFT"),
    CONFIRMED("CFM"),
    VOIDED("VD"),
    DISCARDED("DSC"),
}
