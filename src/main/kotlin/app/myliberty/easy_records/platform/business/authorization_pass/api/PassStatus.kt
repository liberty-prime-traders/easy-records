package app.myliberty.easy_records.platform.business.authorization_pass.api

import app.myliberty.easy_records.util.enums.HasCode

enum class PassStatus(override val code: String) : HasCode {
    ACTIVE("A"),
    REVOKED("R"),
    EXHAUSTED("E"),
    EXPIRED("X")
}
