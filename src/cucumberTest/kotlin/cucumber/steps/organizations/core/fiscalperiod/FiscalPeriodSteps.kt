package app.myliberty.easy_records.cucumber.steps.organizations.core.fiscalperiod

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.TestDates
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.restassured.response.Response

class FiscalPeriodSteps(private val apiClient: ApiClient) {

  private val objectMapper = jacksonObjectMapper()

  private var openPeriodId: String? = null
  private var openPeriodName = ""

  @Given("the open fiscal period is named {string}")
  fun nameOpenFiscalPeriod(name: String) {
    val renameResponse = renameOpenFiscalPeriod(name)
    check(renameResponse.statusCode == 200) {
      "Failed to rename the open fiscal period: ${renameResponse.statusCode} ${renameResponse.asString()}"
    }
  }

  @Given("the past fiscal period is named {string}")
  fun namePastFiscalPeriod(name: String) {
    val periodsResponse = apiClient.get("/secured/fiscal-periods")
    val periods = objectMapper.readTree(periodsResponse.asString())
    val today = TestDates.today().toString()
    val pastPeriod = checkNotNull(periods.firstOrNull { period -> period.path("endDate").asText() < today }) {
      "No fiscal period ended before today; tag the scenario @past-fiscal-periods: ${periodsResponse.asString()}"
    }
    val renameRequest = mapOf("id" to pastPeriod.path("id").asText(), "name" to name)
    val renameResponse = apiClient.put("/secured/fiscal-periods/rename", renameRequest)
    check(renameResponse.statusCode == 200) {
      "Failed to rename the past fiscal period: ${renameResponse.statusCode} ${renameResponse.asString()}"
    }
  }

  @When("I rename the open fiscal period to {string}")
  fun renameTheOpenFiscalPeriod(name: String) {
    renameOpenFiscalPeriod(name)
  }

  @Given("the open fiscal period is not a year-end period")
  fun openPeriodIsNotYearEnd() {
    val openPeriod = findOpenPeriod()
    check(!openPeriod.path("yearEnd").asBoolean()) {
      "The open fiscal period is a year-end period, so the regular-period refusal messages do not apply"
    }
  }

  @When("I year-end close the open fiscal period")
  fun yearEndCloseOpenFiscalPeriod() {
    apiClient.post("/secured/fiscal-periods/${requireOpenPeriodId()}/year-end-close")
  }

  @When("I close the open fiscal period")
  fun closeOpenFiscalPeriod() {
    apiClient.post("/secured/fiscal-periods/close", listOf(requireOpenPeriodId()))
  }

  private fun renameOpenFiscalPeriod(name: String): Response {
    val openPeriod = findOpenPeriod()
    val periodId = openPeriod.path("id").asText()
    openPeriodId = periodId
    openPeriodName = name
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

}
