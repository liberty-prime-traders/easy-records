package app.myliberty.easy_records.platform.business.db_migration.api

import app.myliberty.easy_records.util.enums.HasCode

enum class MigrationStatus(override val code: String) : HasCode {
    INITIATED("INITIATED"),
    SUCCESS("SUCCESS"),
    PARTIAL("PARTIAL"),
    IGNORED("IGNORED"),
    FAILURE("FAILURE")
}
