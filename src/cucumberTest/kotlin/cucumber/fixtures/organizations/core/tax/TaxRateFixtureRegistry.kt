package app.myliberty.easy_records.cucumber.fixtures.organizations.core.tax

import org.springframework.stereotype.Component

@Component
class TaxRateFixtureRegistry {

  private val idsByLabel = mutableMapOf<String, String>()

  fun register(label: String, id: String) {
    idsByLabel[label] = id
  }

  fun idOf(label: String): String =
    checkNotNull(idsByLabel[label]) {
      "No tax rate labelled \"$label\" was created by a fixture in this scenario"
    }

  fun resolveTokens(text: String): String =
    TOKEN_PATTERN.replace(text) { match -> idOf(match.groupValues[1]) }

  fun clear() {
    idsByLabel.clear()
  }

  companion object {
    private val TOKEN_PATTERN = Regex("\\{\\{taxRate:([^}]+)\\}\\}")
  }
}
