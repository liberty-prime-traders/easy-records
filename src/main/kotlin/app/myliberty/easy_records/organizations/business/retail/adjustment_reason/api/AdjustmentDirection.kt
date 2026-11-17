package app.myliberty.easy_records.organizations.business.adjustment_reason.api

import app.myliberty.easy_records.util.enums.HasCode

enum class AdjustmentDirection(override val code: String) : HasCode {
    DISCOUNT("DISC"),
    SURCHARGE("SRCH"),
    BOTH("BOTH")
}
