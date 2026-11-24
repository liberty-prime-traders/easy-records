package app.myliberty.easy_records.cucumber.steps.platform.tax

import io.cucumber.java.Before
import app.myliberty.easy_records.cucumber.fixtures.platform.PlatformTaxFixtureRegistry

class PlatformTaxScenarioHooks(
  private val platformTaxFixtureRegistry: PlatformTaxFixtureRegistry,
) {

  @Before
  fun clearPlatformTaxFixtures() {
    platformTaxFixtureRegistry.clear()
  }
}
