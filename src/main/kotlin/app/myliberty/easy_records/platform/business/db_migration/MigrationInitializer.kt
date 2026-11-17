package app.myliberty.easy_records.platform.business.db_migration

import app.myliberty.easy_records.organizations.business.core.location.api.LocationDto
import app.myliberty.easy_records.platform.business.db_migration.api.DbMigrationInsertDto
import app.myliberty.easy_records.platform.business.db_migration.api.MigrationStatus
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionDto
import app.myliberty.easy_records.platform.business.organization.api.OrganizationDto
import app.myliberty.easy_records.util.enums.SchemaOwnerType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MigrationInitializer(private val dbMigrationCache: DbMigrationCache) {

    fun createOrganizationMigration(
        organization: OrganizationDto,
        targetDbVersion: DbVersionDto,
        migrationType: MigrationType,
        message: String
    ): DbMigrationDto = dbMigrationCache.create(DbMigrationInsertDto(
        dbVersionId = targetDbVersion.id,
        schemaOwnerId = organization.id,
        schemaOwnerType = SchemaOwnerType.ORGANIZATION,
        status = MigrationStatus.INITIATED,
        migrationType = migrationType,
        message = message
    ))

    fun createLocationMigration(
        location: LocationDto,
        targetDbVersion: DbVersionDto,
        parentMigrationId: UUID
    ): DbMigrationDto = dbMigrationCache.create(DbMigrationInsertDto(
        dbVersionId = targetDbVersion.id,
        schemaOwnerId = location.id,
        schemaOwnerType = SchemaOwnerType.LOCATION,
        status = MigrationStatus.INITIATED,
        migrationType = null,
        message = "Location migration in progress",
        migrationParentId = parentMigrationId
    ))

    fun createRetryMigration(
        originalMigration: DbMigrationDto,
        targetDbVersion: DbVersionDto
    ): DbMigrationDto = dbMigrationCache.create(DbMigrationInsertDto(
        dbVersionId = targetDbVersion.id,
        schemaOwnerId = originalMigration.schemaOwnerId,
        schemaOwnerType = SchemaOwnerType.ORGANIZATION,
        status = MigrationStatus.INITIATED,
        message = "Retry of failed locations from migration ${originalMigration.id}",
        migrationParentId = originalMigration.id,
        migrationType = MigrationType.LOCATIONS_ONLY
    ))
}
