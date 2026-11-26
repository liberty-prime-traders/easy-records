package app.myliberty.easy_records.cucumber.steps.organizations.core.settlement

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactInsertDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactStatus
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.core.contact.api.IdentityType
import app.myliberty.easy_records.organizations.business.core.contact_opening_balance.api.ContactSubledgerDirection
import app.myliberty.easy_records.organizations.business.core.payment_method.api.PaymentMethodInsertDto
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.restassured.response.Response
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OpeningSettlementFixtureSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val responseContext: ResponseContext,
  private val openingSettlementFixtureState: OpeningSettlementFixtureState
) {

  @Given("a suspended customer named {string} exists")
  fun createSuspendedCustomer(companyName: String) {
    val contactInsertDto = ContactInsertDto(
      contactTypes = setOf(ContactType.CUSTOMER),
      identityType = IdentityType.ORGANIZATION,
      companyName = companyName,
      status = ContactStatus.SUSPENDED
    )
    val response = apiClient.post("/secured/contacts", contactInsertDto)
    check(response.statusCode == 200) { "Failed to create contact '$companyName': ${response.asString()}" }
    injectContext.store(TransientKey.CONTACT, ResponseContext.idFromResponse(response))
    injectContext.storeText(TransientKey.CONTACT_REFERENCE, response.jsonPath().getString("referenceNumber"))
  }

  @Given("a payment method named {string} on account {string} exists")
  fun createPaymentMethod(name: String, accountCode: String) {
    val paymentMethodInsertDto = PaymentMethodInsertDto(name = name, accountCode = accountCode)
    val response = apiClient.post("/secured/payment-methods", paymentMethodInsertDto)
    check(response.statusCode == 200) {
      "Payment method fixture failed: ${response.statusCode} ${response.asString()}"
    }
    openingSettlementFixtureState.rememberPaymentMethodId(ResponseContext.idFromResponse(response))
  }

  @Given("the customer has an opening receivable of {string}")
  fun setOpeningReceivable(amount: String) {
    setOpeningBalance("receivable", amount)
  }

  @Given("the customer has an opening payable of {string}")
  fun setOpeningPayable(amount: String) {
    setOpeningBalance("payable", amount)
  }

  @When("I record a {word} settlement of {string} dated {string}")
  fun recordSettlement(direction: String, amount: String, settlementDate: String) {
    val contactId = injectContext.get(TransientKey.CONTACT)
    val subledgerDirection = ContactSubledgerDirection.valueOf(direction.uppercase())
    val settlementCreateRequest = mapOf(
      "contactId" to contactId.toString(),
      "subledgerDirection" to subledgerDirection.name,
      "paymentMethodId" to openingSettlementFixtureState.requirePaymentMethodId().toString(),
      "amount" to BigDecimal(amount),
      "settlementDate" to injectContext.inject(settlementDate)
    )
    val response = apiClient.post("/secured/contact-opening-settlements", settlementCreateRequest)
    if (response.statusCode == 200) {
      openingSettlementFixtureState.rememberLatestSettlement(response)
    }
  }

  @When("I void the latest settlement with reason {string}")
  fun voidLatestSettlement(reason: String) {
    val settlementVoidRequest = mapOf(
      "settlementId" to openingSettlementFixtureState.requireLatestSettlementId().toString(),
      "reason" to reason
    )
    apiClient.post("/secured/contact-opening-settlements/void", settlementVoidRequest)
  }

  @Then("the settlement error message should be {string}")
  fun verifySettlementErrorMessage(expectedMessage: String) {
    val lastResponse: Response? = responseContext.lastResponse
    assertNotNull(responseContext.lastError, "Expected an error response but status was ${lastResponse?.statusCode}")
    val actualMessage = lastResponse?.jsonPath()?.getString("message")
    val expectedMessageWithReferences = openingSettlementFixtureState.injectLatestSettlementReference(
      injectContext.inject(expectedMessage)
    )
    assertEquals(expectedMessageWithReferences, actualMessage, "Expected error message '$expectedMessageWithReferences'")
  }

  private fun setOpeningBalance(subledgerSide: String, amount: String) {
    val contactId = injectContext.get(TransientKey.CONTACT)
    val response = apiClient.post(
      "/secured/contact-opening-balances/$contactId",
      mapOf(subledgerSide to BigDecimal(amount))
    )
    check(response.statusCode == 200) {
      "Opening balance fixture failed: ${response.statusCode} ${response.asString()}"
    }
  }
}
