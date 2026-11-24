package app.myliberty.easy_records.cucumber.fixtures.organizations.core.tax

import org.springframework.stereotype.Component

enum class OrgTaxKind(val tokenName: String) {
  JURISDICTION_TAX_TYPE("jurisdictionTaxType"),
  ORG_TAX_TYPE("orgTaxType"),
  TAX_ACCOUNT("taxAccount"),
}

@Component
class OrgTaxFixtureRegistry {

  private val valuesByKindAndName = mutableMapOf<Pair<OrgTaxKind, String>, String>()

  fun register(kind: OrgTaxKind, name: String, value: String) {
    valuesByKindAndName[kind to name] = value
  }

  fun valueOf(kind: OrgTaxKind, name: String): String =
    checkNotNull(valuesByKindAndName[kind to name]) {
      "No ${kind.tokenName} named \"$name\" was created by a fixture in this scenario"
    }

  fun resolveTokens(text: String): String =
    TOKEN_PATTERN.replace(text) { match ->
      val kind = OrgTaxKind.entries.first { it.tokenName == match.groupValues[1] }
      valueOf(kind, match.groupValues[2])
    }

  fun clear() {
    valuesByKindAndName.clear()
  }

  companion object {
    private val TOKEN_PATTERN = Regex("\\{\\{(jurisdictionTaxType|orgTaxType|taxAccount):([^}]+)\\}\\}")
  }
}
