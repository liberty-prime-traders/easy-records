package app.myliberty.easy_records.cucumber.steps.organizations.core.contact

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import java.math.BigDecimal

class ContactLedgerSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val responseContext: ResponseContext
) {

  @Given("a contact opening receivable of {string} is declared")
  fun declareOpeningReceivable(receivableAmount: String) {
    declareOpeningBalance(mapOf("receivable" to BigDecimal(receivableAmount)))
  }

  @Given("a contact opening payable of {string} is declared")
  fun declareOpeningPayable(payableAmount: String) {
    declareOpeningBalance(mapOf("payable" to BigDecimal(payableAmount)))
  }

  @When("I request the next ledger page of {int} entries")
  fun requestNextLedgerPage(requestedSize: Int) {
    val currentCursor = checkNotNull(responseContext.lastResponse?.jsonPath()?.getString("currentCursor")) {
      "No ledger page to continue from"
    }
    apiClient.post(ledgerPath(), ledgerPageRequest(currentCursor, requestedSize))
  }

  private fun declareOpeningBalance(openingBalanceRequest: Map<String, BigDecimal>) {
    val contactId = injectContext.get(TransientKey.CONTACT)
    val response = apiClient.post("/secured/contact-opening-balances/$contactId", openingBalanceRequest)
    check(response.statusCode == 200) {
      "Opening balance fixture failed: ${response.statusCode} ${response.asString()}"
    }
  }

  private fun ledgerPath() = "/secured/contacts/${injectContext.get(TransientKey.CONTACT)}/ledger"

  private fun ledgerPageRequest(previousCursor: String, requestedSize: Int) = mapOf(
    "previousCursor" to previousCursor,
    "requestedSize" to requestedSize,
    "parameters" to emptyMap<String, Any>()
  )
}
