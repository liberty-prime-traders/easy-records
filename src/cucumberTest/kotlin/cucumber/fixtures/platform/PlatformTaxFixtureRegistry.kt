package app.myliberty.easy_records.cucumber.fixtures.platform

import org.springframework.stereotype.Component
import java.util.UUID

enum class PlatformTaxKind(val tokenName: String) {
  JURISDICTION_TYPE("jurisdictionType"),
  JURISDICTION("jurisdiction"),
  TAX_TYPE("taxType"),
}

@Component
class PlatformTaxFixtureRegistry {

  private val idsByKindAndName = mutableMapOf<Pair<PlatformTaxKind, String>, UUID>()
  private var latestJurisdictionTypeId: UUID? = null

  fun register(kind: PlatformTaxKind, name: String, id: UUID) {
    idsByKindAndName[kind to name] = id
    if (kind == PlatformTaxKind.JURISDICTION_TYPE) {
      latestJurisdictionTypeId = id
    }
  }

  fun latestJurisdictionTypeId(): UUID =
    checkNotNull(latestJurisdictionTypeId) {
      "A jurisdiction needs a jurisdiction type: add 'a jurisdiction type named ... exists' first"
    }

  fun idOf(kind: PlatformTaxKind, name: String): UUID =
    checkNotNull(idsByKindAndName[kind to name]) {
      "No ${kind.tokenName} named \"$name\" was created by a fixture in this scenario"
    }

  fun resolveTokens(text: String): String =
    TOKEN_PATTERN.replace(text) { match ->
      val kind = PlatformTaxKind.entries.first { it.tokenName == match.groupValues[1] }
      idOf(kind, match.groupValues[2]).toString()
    }

  fun clear() {
    idsByKindAndName.clear()
    latestJurisdictionTypeId = null
  }

  companion object {
    private val TOKEN_PATTERN = Regex("\\{\\{(jurisdictionType|jurisdiction|taxType):([^}]+)\\}\\}")
  }
}
