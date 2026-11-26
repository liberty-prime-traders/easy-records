package app.myliberty.easy_records.cucumber.steps.platform.tax

import io.cucumber.java.en.When
import org.springframework.http.HttpMethod
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext

class PlatformTaxRequestSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
) {

  @When("I {httpMethod} the platform path {path}")
  fun sendRequestToPlatformPath(method: HttpMethod, path: String) {
    apiClient.exchange(method, injectContext.inject(path))
  }

  @When("I {httpMethod} the platform path {path} with payload:")
  fun sendRequestWithPayloadToPlatformPath(method: HttpMethod, path: String, payload: String) {
    apiClient.exchange(
      method,
      injectContext.inject(path),
      body = injectContext.inject(payload),
    )
  }
}
