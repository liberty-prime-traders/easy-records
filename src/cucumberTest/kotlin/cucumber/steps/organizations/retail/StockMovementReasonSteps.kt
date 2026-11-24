package app.myliberty.easy_records.cucumber.steps.organizations.retail

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import app.myliberty.easy_records.cucumber.fixtures.organizations.retail.StockMovementReasonFixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient

class StockMovementReasonSteps(
  private val stockMovementReasonFixtureBuilder: StockMovementReasonFixtureBuilder,
  private val apiClient: ApiClient
) {

  private val objectMapper = jacksonObjectMapper()

  @Given("a stock movement reason named {string} exists")
  fun createNamedStockMovementReason(name: String) {
    stockMovementReasonFixtureBuilder.createNamed(name)
  }

  @When("I PUT the stock movement reason named {string} with payload:")
  fun updateNamedStockMovementReason(name: String, payload: String) {
    val reasonId = stockMovementReasonIdNamed(name)
    val payloadWithId = objectMapper.readValue<MutableMap<String, Any?>>(payload)
    payloadWithId["id"] = reasonId
    apiClient.put(STOCK_MOVEMENT_REASONS_PATH, body = objectMapper.writeValueAsString(payloadWithId))
  }

  @When("I DELETE the stock movement reason named {string}")
  fun deleteNamedStockMovementReason(name: String) {
    apiClient.delete("$STOCK_MOVEMENT_REASONS_PATH/${stockMovementReasonIdNamed(name)}")
  }

  private fun stockMovementReasonIdNamed(name: String): String {
    val listResponse = apiClient.get(STOCK_MOVEMENT_REASONS_PATH)
    val reasons = objectMapper.readValue<List<Map<String, Any?>>>(listResponse.asString())
    val reasonNamed = reasons.find { it["name"] == name }
    val missingMessage = "No stock movement reason named '$name' in the organization"
    return checkNotNull(reasonNamed?.get("id")) { missingMessage }.toString()
  }

  companion object {
    private const val STOCK_MOVEMENT_REASONS_PATH = "/secured/stock-movement-reasons"
  }
}
