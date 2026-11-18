package app.myliberty.easy_records.locations.business.retail.purchase.api.delivery

import app.myliberty.easy_records.util.enums.HasCode

enum class PurchaseDeliveryStatus(override val code: String) : HasCode {
    PROCESSING("P"),
    RECEIVED("R"),
    FAILED("F");

    fun isAppliedToStock(): Boolean = this == RECEIVED
}
