package app.myliberty.easy_records.util.queries

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Base64
import java.util.UUID

data class KeysetSearchCursor(val createdOn: OffsetDateTime, val id: UUID) {

  fun encode(): String {
    val raw = "$createdOn|$id"
    return Base64.getEncoder().encodeToString(raw.toByteArray())
  }

  companion object {
    fun decode(cursor: String): KeysetSearchCursor? {
      if (cursor.isBlank()) return null
      try {
        val raw = String(Base64.getDecoder().decode(cursor))
        val parts = raw.split("|")
        if (parts.size != 2) throw IllegalArgumentException()
        return KeysetSearchCursor(OffsetDateTime.parse(parts[0]), UUID.fromString(parts[1]))
      } catch (_: IllegalArgumentException) {
        throw LibertyGenericException("Malformed cursor")
      } catch (_: DateTimeParseException) {
        throw LibertyGenericException("Malformed cursor")
      }
    }
  }
}
