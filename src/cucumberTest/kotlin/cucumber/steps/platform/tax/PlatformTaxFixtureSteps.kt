package app.myliberty.easy_records.cucumber.steps.platform.tax

import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.fixtures.platform.JurisdictionFixtureBuilder
import app.myliberty.easy_records.cucumber.fixtures.platform.JurisdictionTypeFixtureBuilder
import app.myliberty.easy_records.cucumber.fixtures.platform.TaxTypeFixtureBuilder
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey

class PlatformTaxFixtureSteps(
  private val jurisdictionTypeFixtureBuilder: JurisdictionTypeFixtureBuilder,
  private val jurisdictionFixtureBuilder: JurisdictionFixtureBuilder,
  private val taxTypeFixtureBuilder: TaxTypeFixtureBuilder,
  private val injectContext: InjectContext,
) {

  @Given("a jurisdiction type named {string} exists")
  fun createNamedJurisdictionType(jurisdictionTypeName: String) {
    val jurisdictionTypeId = jurisdictionTypeFixtureBuilder.createNamed(jurisdictionTypeName)
    injectContext.storeNamed(TransientKey.JURISDICTION_TYPE, jurisdictionTypeName, jurisdictionTypeId)
    injectContext.store(TransientKey.JURISDICTION_TYPE, jurisdictionTypeId)
  }

  @Given("a jurisdiction named {string} exists")
  fun createNamedJurisdiction(jurisdictionName: String) {
    val jurisdictionId = jurisdictionFixtureBuilder.createNamed(jurisdictionName)
    injectContext.storeNamed(TransientKey.JURISDICTION, jurisdictionName, jurisdictionId)
  }

  @Given("a jurisdiction named {string} exists with parent {string}")
  fun createNamedJurisdictionWithParent(jurisdictionName: String, parentJurisdictionName: String) {
    val parentJurisdictionId = injectContext.getNamed(TransientKey.JURISDICTION, parentJurisdictionName)
    val jurisdictionId = jurisdictionFixtureBuilder.createNamed(jurisdictionName, parentJurisdictionId)
    injectContext.storeNamed(TransientKey.JURISDICTION, jurisdictionName, jurisdictionId)
  }

  @Given("a tax type named {string} exists")
  fun createNamedTaxType(taxTypeName: String) {
    val taxTypeId = taxTypeFixtureBuilder.createNamed(taxTypeName)
    injectContext.storeNamed(TransientKey.TAX_TYPE, taxTypeName, taxTypeId)
  }
}
