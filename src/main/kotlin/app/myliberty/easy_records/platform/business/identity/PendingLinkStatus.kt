package app.myliberty.easy_records.platform.business.identity

import app.myliberty.easy_records.util.enums.HasCode

enum class PendingLinkStatus(override val code: String) : HasCode {
    PENDING("P"),
    CONSUMED("C"),
    INVALIDATED("I")
}
