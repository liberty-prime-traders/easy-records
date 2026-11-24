package app.myliberty.easy_records.cucumber.steps.organizations.core.tax

import io.cucumber.java.Before
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.tax.OrgTaxFixtureRegistry

class OrgTaxScenarioHooks(
  private val orgTaxFixtureRegistry: OrgTaxFixtureRegistry,
) {

  @Before
  fun clearOrgTaxFixtures() {
    orgTaxFixtureRegistry.clear()
  }
}
