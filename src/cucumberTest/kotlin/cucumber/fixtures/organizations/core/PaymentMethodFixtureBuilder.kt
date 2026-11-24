package app.myliberty.easy_records.cucumber.fixtures.organizations.core

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.core.payment_method.api.PaymentMethodInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PaymentMethodFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<PaymentMethodInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/payment-methods"

  override fun defaultDto() = PaymentMethodInsertDto(
    name = "Till",
    accountCode = CASH_ACCOUNT_CODE
  )

  fun createNamed(name: String): UUID = create(defaultDto().copy(name = name))

  companion object {
    const val CASH_ACCOUNT_CODE = "001.001.001"
  }
}
