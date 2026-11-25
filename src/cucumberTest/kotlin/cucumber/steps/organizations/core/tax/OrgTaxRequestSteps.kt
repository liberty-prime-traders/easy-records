package app.myliberty.easy_records.cucumber.steps.organizations.core.tax

import io.cucumber.java.en.When
import org.springframework.http.HttpMethod
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext

class OrgTaxRequestSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
) {

  @When("I {httpMethod} the organization tax path {path} with payload:")
  fun sendRequestWithPayloadToOrgTaxPath(method: HttpMethod, path: String, payload: String) {
    apiClient.exchange(
      method,
      path,
      body = injectContext.inject(payload),
    )
  }
}
