package app.myliberty.easy_records.platform.business.db_migration.api

import java.io.Serializable
import java.util.UUID

data class DbMigrationRetryRequestDto(
    val orgMigrationId: UUID,
    val locationIdsToMigrate: Set<UUID>
) : Serializable
