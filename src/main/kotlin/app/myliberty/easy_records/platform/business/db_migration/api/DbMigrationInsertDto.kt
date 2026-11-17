package app.myliberty.easy_records.platform.business.db_migration.api

import app.myliberty.easy_records.platform.business.db_migration.MigrationType
import app.myliberty.easy_records.util.enums.SchemaOwnerType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class DbMigrationInsertDto(
    val dbVersionId: UUID,
    val schemaOwnerId: UUID,
    val schemaOwnerType: SchemaOwnerType,
    val startOn: OffsetDateTime = OffsetDateTime.now(),
    val status: MigrationStatus,
    val migrationType: MigrationType? = null,
    val message: String? = null,
    val migrationParentId: UUID? = null
) : Serializable
