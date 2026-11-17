package app.myliberty.easy_records.platform.business.authorization_pass.api

import app.myliberty.easy_records.util.enums.HasCode

enum class PassType(override val code: String) : HasCode {
    CREATE_ORGANIZATION("CRORG")
}
