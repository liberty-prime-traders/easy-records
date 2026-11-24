package app.myliberty.easy_records.cucumber.steps.organizations.core.tax

import io.cucumber.java.en.When
import org.springframework.http.HttpMethod
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.tax.OrgTaxFixtureRegistry
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.tax.TaxRateFixtureRegistry
import app.myliberty.easy_records.cucumber.support.ApiClient

class TaxRateRequestSteps(
  private val apiClient: ApiClient,
  private val orgTaxFixtureRegistry: OrgTaxFixtureRegistry,
  private val taxRateFixtureRegistry: TaxRateFixtureRegistry,
) {

  @When("I {httpMethod} the tax rate path {path} with payload:")
  fun sendRequestWithPayloadToTaxRatePath(method: HttpMethod, path: String, payload: String) {
    val resolvedPayload = taxRateFixtureRegistry.resolveTokens(orgTaxFixtureRegistry.resolveTokens(payload))
    apiClient.exchange(method, path, body = resolvedPayload)
  }
}
