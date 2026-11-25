package app.myliberty.easy_records.cucumber.steps.organizations.core.tax

import io.cucumber.java.en.When
import org.springframework.http.HttpMethod
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext

class TaxRateRequestSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
) {

  @When("I {httpMethod} the tax rate path {path} with payload:")
  fun sendRequestWithPayloadToTaxRatePath(method: HttpMethod, path: String, payload: String) {
    apiClient.exchange(method, path, body = injectContext.inject(payload))
  }
}
