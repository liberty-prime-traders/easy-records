package app.myliberty.easy_records.platform.business.db_migration

data class OrganizationLocationsMigration(
    val organizationMigration: DbMigrationDto,
    val locationMigrations: List<DbMigrationDto>
)
