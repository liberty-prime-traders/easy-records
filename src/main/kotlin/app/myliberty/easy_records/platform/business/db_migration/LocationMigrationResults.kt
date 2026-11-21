package app.myliberty.easy_records.platform.business.db_migration

data class LocationMigrationResults(
  val successful: List<DbMigrationDto>,
  val failed: List<DbMigrationDto>
) {
  companion object {
    fun empty() = LocationMigrationResults(emptyList(), emptyList())
  }

  fun getFailedMessages(): String {
    return failed.joinToString("; ") { "(ID: ${it.id}): ${it.message ?: "Unknown error"}" }
  }

  fun getAllResults(): List<DbMigrationDto> = successful + failed
}
