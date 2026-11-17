package app.myliberty.easy_records.locations.business.sale_adjustment.api

import app.myliberty.easy_records.util.enums.HasCode

enum class PriceAdjustmentMethod(override val code: String) : HasCode {
    PERCENTAGE("PCT"),

    /** The value is the unit price the cashier typed; the amount is the line total minus that price x quantity. Only a price override uses it. */
    UNIT_PRICE_OVERRIDE("UPO")
}
