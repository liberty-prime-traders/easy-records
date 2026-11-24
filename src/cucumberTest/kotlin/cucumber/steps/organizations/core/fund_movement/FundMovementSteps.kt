package app.myliberty.easy_records.cucumber.steps.organizations.core.fund_movement

import app.myliberty.easy_records.cucumber.support.ApiClient
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class FundMovementSteps(
  private val apiClient: ApiClient
) {

  private val cashAccountCode = "001.001.001"

  private var lastOwnerFundMovementId: UUID? = null

  @Given("the owner has contributed {string} of cash")
  fun contributeCapitalToCash(amount: String) {
    val capitalCreateRequest = mapOf(
      "accountCode" to cashAccountCode,
      "amount" to BigDecimal(amount),
      "movementDate" to today().toString()
    )
    val capitalResponse = apiClient.post("/secured/fund-movements/capital", capitalCreateRequest)
    check(capitalResponse.statusCode == 200) {
      "Fund movement fixture failed: ${capitalResponse.statusCode} ${capitalResponse.asString()}"
    }
    lastOwnerFundMovementId = UUID.fromString(capitalResponse.jsonPath().getString("id"))
  }

  @When("I void the owner's fund movement with reason {string}")
  fun voidOwnerFundMovement(reason: String) {
    val fundMovementId = checkNotNull(lastOwnerFundMovementId) { "No owner fund movement has been created" }
    val voidRequest = mapOf("id" to fundMovementId.toString(), "reason" to reason)
    apiClient.post("/secured/fund-movements/void", voidRequest)
  }

  private fun today(): LocalDate = LocalDate.now(ZoneOffset.UTC)
}
