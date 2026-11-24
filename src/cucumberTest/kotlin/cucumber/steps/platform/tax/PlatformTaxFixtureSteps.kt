package app.myliberty.easy_records.cucumber.steps.platform.tax

import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.fixtures.platform.JurisdictionFixtureBuilder
import app.myliberty.easy_records.cucumber.fixtures.platform.JurisdictionTypeFixtureBuilder
import app.myliberty.easy_records.cucumber.fixtures.platform.PlatformTaxFixtureRegistry
import app.myliberty.easy_records.cucumber.fixtures.platform.PlatformTaxKind
import app.myliberty.easy_records.cucumber.fixtures.platform.TaxTypeFixtureBuilder

class PlatformTaxFixtureSteps(
  private val jurisdictionTypeFixtureBuilder: JurisdictionTypeFixtureBuilder,
  private val jurisdictionFixtureBuilder: JurisdictionFixtureBuilder,
  private val taxTypeFixtureBuilder: TaxTypeFixtureBuilder,
  private val platformTaxFixtureRegistry: PlatformTaxFixtureRegistry,
) {

  @Given("a jurisdiction type named {string} exists")
  fun createNamedJurisdictionType(jurisdictionTypeName: String) {
    val jurisdictionTypeId = jurisdictionTypeFixtureBuilder.createNamed(jurisdictionTypeName)
    platformTaxFixtureRegistry.register(PlatformTaxKind.JURISDICTION_TYPE, jurisdictionTypeName, jurisdictionTypeId)
  }

  @Given("a jurisdiction named {string} exists")
  fun createNamedJurisdiction(jurisdictionName: String) {
    val jurisdictionId = jurisdictionFixtureBuilder.createNamed(jurisdictionName)
    platformTaxFixtureRegistry.register(PlatformTaxKind.JURISDICTION, jurisdictionName, jurisdictionId)
  }

  @Given("a jurisdiction named {string} exists with parent {string}")
  fun createNamedJurisdictionWithParent(jurisdictionName: String, parentJurisdictionName: String) {
    val parentJurisdictionId = platformTaxFixtureRegistry.idOf(PlatformTaxKind.JURISDICTION, parentJurisdictionName)
    val jurisdictionId = jurisdictionFixtureBuilder.createNamed(jurisdictionName, parentJurisdictionId)
    platformTaxFixtureRegistry.register(PlatformTaxKind.JURISDICTION, jurisdictionName, jurisdictionId)
  }

  @Given("a tax type named {string} exists")
  fun createNamedTaxType(taxTypeName: String) {
    val taxTypeId = taxTypeFixtureBuilder.createNamed(taxTypeName)
    platformTaxFixtureRegistry.register(PlatformTaxKind.TAX_TYPE, taxTypeName, taxTypeId)
  }
}
