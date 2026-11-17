package app.myliberty.easy_records.organizations.business.accounting_config.api

import app.myliberty.easy_records.util.enums.HasCode

enum class FiscalPeriodCycle(override val code: String) : HasCode {
    WEEKLY("W"),
    MONTHLY("M"),
    QUARTERLY("Q"),
    SEMI_ANNUAL("SA"),
    ANNUAL("A"),
    FOUR_FOUR_FIVE("445")
}
