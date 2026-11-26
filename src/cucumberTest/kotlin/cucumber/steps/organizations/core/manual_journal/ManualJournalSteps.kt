package app.myliberty.easy_records.cucumber.steps.organizations.core.manual_journal

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.math.BigDecimal
import kotlin.test.assertEquals

class ManualJournalSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val responseContext: ResponseContext
) {

  private var rememberedReferenceNumber: String? = null

  @Given("a manual journal of {string} for rent exists")
  fun createManualJournalForRent(amount: String) {
    val manualJournalCreateRequest = mapOf(
      "summary" to "Rent accrued against owner's capital",
      "details" to "Month-end accrual",
      "postingDate" to injectContext.inject("{{today}}"),
      "lines" to listOf(
        mapOf(
          "accountCode" to "005.003",
          "entryType" to "DEBIT",
          "amount" to BigDecimal(amount),
          "lineMemo" to "November rent"
        ),
        mapOf(
          "accountCode" to "003.001",
          "entryType" to "CREDIT",
          "amount" to BigDecimal(amount),
          "lineMemo" to "Capital"
        )
      )
    )
    val response = apiClient.post("/secured/manual-journals", manualJournalCreateRequest)
    check(response.statusCode == 200) { "Manual journal fixture failed: ${response.statusCode} ${response.asString()}" }
    val createdReferenceNumber = response.jsonPath().getString("referenceNumber")
    rememberedReferenceNumber = createdReferenceNumber
    injectContext.storeText(TransientKey.MANUAL_JOURNAL, createdReferenceNumber)
  }

  @When("I reverse the last manual journal with reason {string}")
  fun reverseLastManualJournal(reason: String) {
    val lastReferenceNumber = checkNotNull(lastReferenceNumberInResponse()) {
      "No manual journal reference in the last response"
    }
    rememberedReferenceNumber = lastReferenceNumber
    postReversal(lastReferenceNumber, reason)
  }

  @When("I reverse the remembered manual journal again with reason {string}")
  fun reverseRememberedManualJournalAgain(reason: String) {
    val remembered = checkNotNull(rememberedReferenceNumber) { "No manual journal was reversed in this scenario" }
    postReversal(remembered, reason)
  }

  @Then("the response should reverse the remembered manual journal")
  fun verifyResponseReversesRememberedManualJournal() {
    val reversedReferenceNumber = responseContext.lastResponse?.jsonPath()?.getString("reversalOfReferenceNumber")
    assertEquals(
      rememberedReferenceNumber,
      reversedReferenceNumber,
      "Reversal should point at the remembered manual journal"
    )
  }

  private fun lastReferenceNumberInResponse(): String? =
    responseContext.lastResponse?.jsonPath()?.getString("referenceNumber")

  private fun postReversal(referenceNumber: String, reason: String) {
    val response = apiClient.post("/secured/manual-journals/$referenceNumber/reversal", mapOf("reason" to reason))
    if (response.statusCode == 200) {
      injectContext.storeText(TransientKey.MANUAL_JOURNAL_REVERSAL, response.jsonPath().getString("referenceNumber"))
    }
  }
}
