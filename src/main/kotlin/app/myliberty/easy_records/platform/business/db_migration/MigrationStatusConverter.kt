package app.myliberty.easy_records.platform.business.db_migration

import jakarta.persistence.Converter
import app.myliberty.easy_records.platform.business.db_migration.api.MigrationStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class MigrationStatusConverter : EnumConverter<MigrationStatus>(MigrationStatus::class.java)
