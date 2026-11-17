package app.myliberty.easy_records.locations.business.retail.purchase.api

import app.myliberty.easy_records.util.enums.HasCode

enum class PurchaseStatus(override val code: String) : HasCode {
    DRAFT("DFT"),
    ORDERED("ORD"),
    PARTIALLY_DELIVERED("PDL"),
    FULLY_DELIVERED("FDL"),
    CANCELED("CLD");
}
