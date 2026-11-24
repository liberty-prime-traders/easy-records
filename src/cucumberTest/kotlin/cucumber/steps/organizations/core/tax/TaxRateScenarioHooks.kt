package app.myliberty.easy_records.cucumber.steps.organizations.core.tax

import io.cucumber.java.Before
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.tax.TaxRateFixtureRegistry

class TaxRateScenarioHooks(
  private val taxRateFixtureRegistry: TaxRateFixtureRegistry,
) {

  @Before
  fun clearTaxRateFixtures() {
    taxRateFixtureRegistry.clear()
  }
}
