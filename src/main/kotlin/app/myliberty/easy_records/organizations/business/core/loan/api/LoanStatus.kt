package app.myliberty.easy_records.organizations.business.core.loan.api

import app.myliberty.easy_records.util.enums.HasCode

enum class LoanStatus(override val code: String) : HasCode {
    OPEN("OPN"),
    SETTLED("STL"),
    VOIDED("VOI")
}
