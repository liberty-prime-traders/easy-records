package app.myliberty.easy_records.organizations.business.core.fund_movement.api

import app.myliberty.easy_records.util.enums.HasCode

enum class FundMovementType(override val code: String) : HasCode {
    TRANSFER_BETWEEN_ACCOUNTS("TRF"),
    CONTRIBUTE_CAPITAL_TO_BUSINESS("CPT"),
    WITHDRAW_FOR_PERSONAL_USE("DRW")
}
