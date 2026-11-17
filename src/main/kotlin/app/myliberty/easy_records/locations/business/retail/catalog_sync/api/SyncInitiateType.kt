package app.myliberty.easy_records.locations.business.catalog_sync.api

import app.myliberty.easy_records.util.enums.HasCode

enum class SyncInitiateType(override val code: String) : HasCode {
    USER("USR"),
    SCHEDULE("SCH")
}
