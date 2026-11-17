package app.myliberty.easy_records.platform.business.identity.api

import app.myliberty.easy_records.util.enums.HasCode

enum class IdentityProvider(override val code: String) : HasCode {
    GOOGLE("GGL")
}
