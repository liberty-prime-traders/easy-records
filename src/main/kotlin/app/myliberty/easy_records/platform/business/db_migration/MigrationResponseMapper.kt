package app.myliberty.easy_records.platform.business.db_migration

import app.myliberty.easy_records.platform.business.db_migration.api.OrganizationMigrationResponseDto
import org.springframework.stereotype.Component

@Component
class MigrationResponseMapper(private val dbMigrationMapper: DbMigrationMapper) {

  fun toOrganizationResponse(migration: OrganizationLocationsMigration): OrganizationMigrationResponseDto {
    return dbMigrationMapper.toOrganizationResponseDto(migration.organizationMigration).apply {
      locations = migration.locationMigrations.map {
        dbMigrationMapper.toLocationResponseDto(it)
      }
    }
  }
}
