package app.myliberty.easy_records.organizations.business.location.api

import app.myliberty.easy_records.util.enums.HasCode

enum class LocationType(override val code: String) : HasCode {
    SHOP("SHP"),
    STORE("STR")
}
