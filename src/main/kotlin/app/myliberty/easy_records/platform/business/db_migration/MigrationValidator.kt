package app.myliberty.easy_records.platform.business.db_migration

import app.myliberty.easy_records.platform.business.db_migration.api.DbMigrationRequestDto
import app.myliberty.easy_records.platform.business.db_migration.api.DbMigrationRetryRequestDto
import app.myliberty.easy_records.platform.business.db_migration.api.MigrationStatus
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionDto
import app.myliberty.easy_records.platform.business.db_version.api.DbVersionService
import app.myliberty.easy_records.util.enums.SchemaOwnerType
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MigrationValidator(
  private val dbVersionService: DbVersionService,
  private val dbMigrationCache: DbMigrationCache,
) {
  fun validateMigrationRequest(request: DbMigrationRequestDto): DbVersionDto {
    val targetVersion = getActiveTargetVersion(request.targetDbVersionId)
    validatePreviousMigrationCompleted(request.organizationId, targetVersion)
    return targetVersion
  }

  fun validateRetryRequest(request: DbMigrationRetryRequestDto): Pair<DbMigrationDto, DbVersionDto> {
    val originalMigration = getOriginalMigration(request.orgMigrationId)
    validateRetryEligibility(originalMigration)
    val targetVersion = getTargetVersion(originalMigration.dbVersionId)
    return Pair(originalMigration, targetVersion)
  }

  private fun getActiveTargetVersion(versionId: UUID): DbVersionDto {
    val version = dbVersionService.getAllDbVersionDtos().find { it.id == versionId }
      ?: throw LibertyGenericException("Target DB version not found")

    version.activatedOn ?: throw LibertyGenericException("Target DB version is inactive")

    return version
  }

  private fun getTargetVersion(versionId: UUID): DbVersionDto {
    return dbVersionService.getAllDbVersionDtos().find { it.id == versionId }
      ?: throw LibertyGenericException("Target DB version not found")
  }

  private fun validatePreviousMigrationCompleted(organizationId: UUID, targetVersion: DbVersionDto) {
    val prevVersionId = targetVersion.prevVersionId ?: return

    val prevVersion = dbVersionService.getAllDbVersionDtos().find { it.id == prevVersionId }
      ?: return

    val previousMigration = dbMigrationCache.getTopBySchemaOwnerIdAndSchemaOwnerTypeAndDbVersionIdOrderByStartOnDesc(
      organizationId,
      SchemaOwnerType.ORGANIZATION,
      prevVersionId
    )

    when {
      previousMigration == null -> throw LibertyGenericException(
        "Organization must first migrate to version ${prevVersion.versionNumber}"
      )
      previousMigration.status != MigrationStatus.SUCCESS -> throw LibertyGenericException(
        "Previous migration attempt (Version: ${prevVersion.versionNumber}) was not successful."
      )
    }
  }

  private fun getOriginalMigration(migrationId: UUID): DbMigrationDto {
    return dbMigrationCache.getAllDbMigrations().find { it.id == migrationId }
      ?: throw LibertyGenericException("Original DB migration not found")
  }

  private fun validateRetryEligibility(migration: DbMigrationDto) {
    if (migration.schemaOwnerType != SchemaOwnerType.ORGANIZATION) {
      throw LibertyGenericException("Only organization-level migrations can be retried")
    }
    if (migration.status != MigrationStatus.PARTIAL) {
      throw LibertyGenericException("Only partially completed migrations can be retried")
    }
  }
}
