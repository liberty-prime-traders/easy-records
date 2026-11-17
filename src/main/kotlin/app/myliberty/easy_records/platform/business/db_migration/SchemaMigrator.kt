package app.myliberty.easy_records.platform.business.db_migration

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component

@Component
class SchemaMigrator(
  private val schemaExecutor: SchemaExecutor,
  private val migrationStatusUpdater: MigrationStatusUpdater
) {
  fun migrateOrganizationSchema(
    schemaName: String,
    entityName: String,
    versionLabel: String,
    previousVersionLabel: String? = null
  ) {
    try {
      schemaExecutor.executeOrganizationSchema(schemaName, versionLabel, previousVersionLabel)
    } catch (e: Exception) {
      throw LibertyGenericException("$entityName schema migration failed: ${e.message}")
    }
  }

  fun migrateLocationSchema(
    schemaName: String,
    migration: DbMigrationDto,
    entityName: String,
    versionLabel: String,
    previousVersionLabel: String? = null
  ) {
    try {
      schemaExecutor.executeLocationSchema(schemaName, versionLabel, previousVersionLabel)
      migrationStatusUpdater.markSuccess(migration, "Successfully migrated")
    } catch (e: Exception) {
      migrationStatusUpdater.markFailure(migration, e)
      throw LibertyGenericException("$entityName migration failed: ${e.message}")
    }
  }
}
