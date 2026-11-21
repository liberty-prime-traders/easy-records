package app.myliberty.easy_records.locations.business.retail.purchase.api.delivery

import app.myliberty.easy_records.util.enums.HasCode

enum class PurchaseDeliveryLineStatus(override val code: String) : HasCode {
    ACTIVE("ACT"),
    REVISED("RVS")
}
