package app.myliberty.easy_records.platform.business.db_migration

import app.myliberty.easy_records.util.enums.HasCode

enum class MigrationType(override val code: String) : HasCode {
    ORG_ONLY("ORG_ONLY"),
    ORG_WITH_LOCATIONS("ORG_WITH_LOCATIONS"),
    LOCATIONS_ONLY("LOCATIONS")
}
