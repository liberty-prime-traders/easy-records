package app.myliberty.easy_records.cucumber.support.context

import app.myliberty.easy_records.cucumber.support.TestDates
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InjectContext {

  private val multiStore = mutableMapOf<String, MutableList<String>>()
  private val persistentStore = mutableMapOf<String, MutableList<String>>()

  fun store(key: ContextKey, value: UUID) {
    storeText(key, value.toString())
  }

  fun storeText(key: ContextKey, value: String) {
    multiStore.getOrPut(key.key) { mutableListOf() }.add(value)
  }

  fun persist(key: PersistentKey, value: UUID) {
    persistentStore.getOrPut(key.key) { mutableListOf() }.add(value.toString())
  }

  fun find(key: ContextKey, index: Int? = null): UUID? = findString(key, index)?.let { UUID.fromString(it) }

  fun get(key: ContextKey, index: Int? = null): UUID =
    checkNotNull(find(key, index)) { "No value found in context for key '${key.key}'" }

  fun storeNamed(kind: TransientKey, name: String, value: UUID) {
    storeNamedText(kind, name, value.toString())
  }

  fun storeNamedText(kind: TransientKey, name: String, value: String) {
    multiStore.getOrPut(namedKey(kind, name)) { mutableListOf() }.add(value)
  }

  fun getNamed(kind: TransientKey, name: String): UUID = UUID.fromString(getNamedText(kind, name))

  fun getNamedText(kind: TransientKey, name: String): String =
    checkNotNull(multiStore[namedKey(kind, name)]?.lastOrNull()) {
      "No ${kind.key} named \"$name\" was created by a fixture in this scenario"
    }

  fun findString(key: ContextKey, index: Int? = null): String? {
    val store = when (key) {
      is PersistentKey -> persistentStore
      is TransientKey -> multiStore
    }
    return store[key.key]?.let { if (index == null) it.lastOrNull() else it.getOrNull(index) }
  }

  fun inject(text: String): String =
    PLACEHOLDER_PATTERN.replace(text) { match -> resolvePlaceholder(match.groupValues[1]) ?: match.value }

  private fun resolvePlaceholder(token: String): String? {
    if (token == TODAY) return TestDates.today().toString()
    if (token == TODAY_IN_DATABASE_ZONE) return TestDates.todayInDatabaseZone().toString()
    if (token == NOW) return TestDates.now().toString()
    if (token == DAY_AGO) return TestDates.now().minusDays(1).toString()
    if (token == HALF_DAY_AGO) return TestDates.now().minusHours(12).toString()
    if (token == TWO_DAYS_AGO) return TestDates.now().minusDays(2).toString()
    if (token == DAY_AHEAD) return TestDates.now().plusDays(1).toString()
    if (token.contains(':')) {
      val kindToken = token.substringBefore(':')
      val kind = KEY_LOOKUP[kindToken] as? TransientKey ?: error("Unknown named fixture kind '$kindToken'")
      return getNamedText(kind, token.substringAfter(':'))
    }
    val parts = token.split("->")
    val key = KEY_LOOKUP[parts[0]] ?: return null
    return findString(key, parts.getOrNull(1)?.toIntOrNull())
  }

  fun rememberText(keyName: String, value: String) {
    val key = KEY_LOOKUP[keyName] as? TransientKey ?: error("Unknown fixture key '$keyName'")
    storeText(key, value)
  }

  fun clear() = multiStore.clear()

  private fun namedKey(kind: TransientKey, name: String): String = "${kind.key}:$name"

  companion object {
    private const val TODAY = "today"
    private const val TODAY_IN_DATABASE_ZONE = "todayUtc"
    private const val NOW = "now"
    private const val DAY_AGO = "dayAgo"
    private const val HALF_DAY_AGO = "halfDayAgo"
    private const val TWO_DAYS_AGO = "twoDaysAgo"
    private const val DAY_AHEAD = "dayAhead"
    private val PLACEHOLDER_PATTERN = Regex("\\{\\{([^{}]+)}}")
    private val KEY_LOOKUP: Map<String, ContextKey> = buildMap {
      PersistentKey.entries.forEach { put(it.key, it) }
      TransientKey.entries.forEach { put(it.key, it) }
    }
  }
}
