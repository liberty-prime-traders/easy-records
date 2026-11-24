package app.myliberty.easy_records.cucumber.steps.organizations.core.fiscalperiod

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.restassured.response.Response
import kotlin.test.assertEquals

class FiscalPeriodSteps(
  private val apiClient: ApiClient,
  private val responseContext: ResponseContext
) {

  private val objectMapper = jacksonObjectMapper()

  private var openPeriodId: String? = null
  private var openPeriodName = ""
  private var openPeriodIsYearEnd = false

  @Given("the open fiscal period is named {string}")
  fun nameOpenFiscalPeriod(name: String) {
    val renameResponse = renameOpenFiscalPeriod(name)
    check(renameResponse.statusCode == 200) {
      "Failed to rename the open fiscal period: ${renameResponse.statusCode} ${renameResponse.asString()}"
    }
  }

  @When("I rename the open fiscal period to {string}")
  fun renameTheOpenFiscalPeriod(name: String) {
    renameOpenFiscalPeriod(name)
  }

  @When("I close the open fiscal period")
  fun closeOpenFiscalPeriod() {
    apiClient.post("/secured/fiscal-periods/close", listOf(requireOpenPeriodId()))
  }

  @When("I year-end close the open fiscal period")
  fun yearEndCloseOpenFiscalPeriod() {
    apiClient.post("/secured/fiscal-periods/${requireOpenPeriodId()}/year-end-close")
  }

  // The close endpoint refuses a year-end period before the ended-date rule; a regular period reaches that rule.
  @Then("the response error message should be the close refusal for the open fiscal period")
  fun verifyCloseRefusal() {
    val expectedMessage = if (openPeriodIsYearEnd) {
      "Period '$openPeriodName' is a year-end period — use the year-end close endpoint"
    } else {
      "Period '$openPeriodName' has not ended yet and cannot be closed"
    }
    verifyErrorMessage(expectedMessage)
  }

  // The year-end endpoint refuses a regular period before it checks the ended-date rule.
  @Then("the response error message should be the year-end refusal for the open fiscal period")
  fun verifyYearEndRefusal() {
    val expectedMessage = if (openPeriodIsYearEnd) {
      "Period '$openPeriodName' has not ended yet and cannot be closed"
    } else {
      "Period '$openPeriodName' is not a year-end period"
    }
    verifyErrorMessage(expectedMessage)
  }

  private fun renameOpenFiscalPeriod(name: String): Response {
    val openPeriod = findOpenPeriod()
    val periodId = openPeriod.path("id").asText()
    openPeriodId = periodId
    openPeriodName = name
    openPeriodIsYearEnd = openPeriod.path("yearEnd").asBoolean()
    val renameRequest = mapOf("id" to periodId, "name" to name)
    return apiClient.put("/secured/fiscal-periods/rename", renameRequest)
  }

  private fun findOpenPeriod(): JsonNode {
    val periodsResponse = apiClient.get("/secured/fiscal-periods")
    val periods = objectMapper.readTree(periodsResponse.asString())
    return checkNotNull(periods.firstOrNull { period -> period.path("current").asBoolean() }) {
      "No fiscal period covers today: ${periodsResponse.asString()}"
    }
  }

  private fun requireOpenPeriodId(): String =
    checkNotNull(openPeriodId) { "The open fiscal period has not been named yet" }

  private fun verifyErrorMessage(expectedMessage: String) {
    val actualMessage = responseContext.lastResponse?.jsonPath()?.getString("message")
    assertEquals(expectedMessage, actualMessage)
  }
}
