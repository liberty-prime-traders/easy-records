package app.myliberty.easy_records.platform.business.sysuser.api

import app.myliberty.easy_records.util.enums.HasCode

enum class UserType(override val code: String) : HasCode {
    END_USER("E"),
    SERVICE_ACCOUNT("S")
}
