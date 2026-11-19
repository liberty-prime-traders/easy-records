package app.myliberty.easy_records.platform.business.db_migration

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class MigrationTypeConverter : EnumConverter<MigrationType>(MigrationType::class.java)
