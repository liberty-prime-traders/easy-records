package app.myliberty.easy_records.cucumber.support.context

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Component
class InjectContext {

  private val multiStore = mutableMapOf<String, MutableList<String>>()
  private val persistentStore = mutableMapOf<String, MutableList<String>>()

  fun store(key: TransientKey, value: UUID) {
    multiStore.getOrPut(key.key) { mutableListOf() }.add(value.toString())
  }

  fun storeText(key: TransientKey, value: String) {
    multiStore.getOrPut(key.key) { mutableListOf() }.add(value)
  }

  fun persist(key: PersistentKey, value: UUID) {
    persistentStore.getOrPut(key.key) { mutableListOf() }.add(value.toString())
  }

  fun find(key: ContextKey, index: Int? = null): UUID? = findString(key, index)?.let { UUID.fromString(it) }

  fun get(key: ContextKey, index: Int? = null): UUID = checkNotNull(find(key, index)) { "No value found in context for key '${key.key}'" }

  fun findString(key: ContextKey, index: Int? = null): String? {
    val store = when (key) {
      is PersistentKey -> persistentStore
      is TransientKey -> multiStore
    }
    return store[key.key]?.let { if (index == null) it.lastOrNull() else it.getOrNull(index) }
  }

  fun inject(text: String): String =
    PLACEHOLDER_PATTERN.replace(text) { match ->
      val parts = match.groupValues[1].split("->")
      val keyStr = parts[0]
      if (keyStr == TODAY) return@replace LocalDate.now(ZoneOffset.UTC).toString()
      val index = parts.getOrNull(1)?.toIntOrNull()
      KEY_LOOKUP[keyStr]?.let { findString(it, index) } ?: match.value
    }

  fun clear() = multiStore.clear()

  companion object {
    private const val TODAY = "today"
    private val PLACEHOLDER_PATTERN = Regex("#([^\"\\s.!?,:;()\\[\\]{}/_]+)")
    private val KEY_LOOKUP: Map<String, ContextKey> = buildMap {
      PersistentKey.entries.forEach { put(it.key, it) }
      TransientKey.entries.forEach { put(it.key, it) }
    }
  }
}
