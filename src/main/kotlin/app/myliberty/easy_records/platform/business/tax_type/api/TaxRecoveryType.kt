package app.myliberty.easy_records.platform.business.tax_type.api

import app.myliberty.easy_records.util.enums.HasCode

enum class TaxRecoveryType(override val code: String) : HasCode {
    RECOVERABLE("REC"),
    NON_RECOVERABLE("NREC")
}
