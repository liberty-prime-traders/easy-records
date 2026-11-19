package app.myliberty.easy_records.cucumber.support.assertions

import java.util.UUID

enum class SchemaScope {
  PLATFORM,
  ORGANIZATION,
}

interface PersistedResponseBinding {
  val alias: String
  val scope: SchemaScope

  fun responseDtoFor(id: UUID): Any
}
