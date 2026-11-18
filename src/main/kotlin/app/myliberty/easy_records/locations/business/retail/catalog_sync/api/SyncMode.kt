package app.myliberty.easy_records.locations.business.retail.catalog_sync.api

import app.myliberty.easy_records.util.enums.HasCode

enum class SyncMode(override val code: String) : HasCode {
    FULL("F"),
    INCREMENTAL("I")
}
