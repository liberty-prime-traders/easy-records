package app.myliberty.easy_records.platform.business.db_migration.api

import java.io.Serializable
import java.util.UUID

data class DbMigrationRequestDto(
    val organizationId: UUID,
    val locationIdsToMigrate: Set<UUID>,
    val targetDbVersionId: UUID
) : Serializable
