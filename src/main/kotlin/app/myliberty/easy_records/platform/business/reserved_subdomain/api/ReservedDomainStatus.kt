package app.myliberty.easy_records.platform.business.reserved_subdomain.api

import app.myliberty.easy_records.util.enums.HasCode

enum class ReservedDomainStatus(override val code: String) : HasCode {
    USED("USD"),
    UNUSED("UNSD"),
    ABANDONED("ABND");
}
