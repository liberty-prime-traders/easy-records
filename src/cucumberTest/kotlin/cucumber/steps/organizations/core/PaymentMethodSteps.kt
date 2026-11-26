package app.myliberty.easy_records.cucumber.steps.organizations.core

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.PaymentMethodFixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import org.springframework.http.HttpMethod
import java.util.UUID

// The InjectContext placeholder keys are an enum shared with every domain, so the payment method id is kept here and
// substituted into "{{paymentMethod}}" by the steps below. The scenario's Given always runs before these steps.
class PaymentMethodSteps(
  private val paymentMethodFixtureBuilder: PaymentMethodFixtureBuilder,
  private val apiClient: ApiClient
) {

  private var paymentMethodId: UUID? = null

  @Given("a payment method named {string} exists")
  fun createNamedPaymentMethod(name: String) {
    paymentMethodId = paymentMethodFixtureBuilder.createNamed(name)
  }

  @When("I PUT the payment method with payload:")
  fun updatePaymentMethod(payload: String) {
    val resolvedPayload = payload.replace(PLACEHOLDER, storedPaymentMethodId())
    apiClient.exchange(HttpMethod.PUT, PAYMENT_METHODS_PATH, body = resolvedPayload)
  }

  @When("I DELETE the payment method")
  fun deletePaymentMethod() {
    apiClient.exchange(HttpMethod.DELETE, "$PAYMENT_METHODS_PATH/${storedPaymentMethodId()}")
  }

  private fun storedPaymentMethodId(): String =
    checkNotNull(paymentMethodId) { "No payment method was created in this scenario" }.toString()

  private companion object {
    const val PAYMENT_METHODS_PATH = "/secured/payment-methods"
    const val PLACEHOLDER = "{{paymentMethod}}"
  }
}
