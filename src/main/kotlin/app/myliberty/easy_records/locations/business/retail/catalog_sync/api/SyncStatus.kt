package app.myliberty.easy_records.locations.business.retail.catalog_sync.api

import app.myliberty.easy_records.util.enums.HasCode

enum class SyncStatus(override val code: String) : HasCode {
    IN_PROGRESS("I"),
    COMPLETED("C"),
    FAILED("F"),
    CANCELLATION_REQUESTED("XR"),
    CANCELLED("X")
}
