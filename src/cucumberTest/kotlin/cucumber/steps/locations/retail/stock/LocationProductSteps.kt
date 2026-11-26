package app.myliberty.easy_records.cucumber.steps.locations.retail.stock

import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.AsyncPosting
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import app.myliberty.easy_records.util.paging.PageRequest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

// The location product only exists once the catalog consumer has synced the organization product, hence the poll.
class LocationProductSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  private val objectMapper = jacksonObjectMapper()

  @Given("the location product named {string} is available")
  fun rememberLocationProduct(productName: String) {
    AsyncPosting.untilAsserted("location product named $productName in the catalog") {
      val locationProduct = locationProductNamed(productName)
      val locationProductId = UUID.fromString(locationProduct.path("id").asText())
      val locationProductReference = locationProduct.path("referenceNumber").asText()
      injectContext.store(TransientKey.LOCATION_PRODUCT, locationProductId)
      injectContext.storeNamed(TransientKey.LOCATION_PRODUCT, productName, locationProductId)
      injectContext.storeNamedText(TransientKey.LOCATION_PRODUCT_REFERENCE, productName, locationProductReference)
    }
  }

  @Given("the location product named {string} is deactivated")
  fun deactivate(productName: String) {
    val locationProductId = injectContext.getNamed(TransientKey.LOCATION_PRODUCT, productName)
    val response = apiClient.put("/secured/location-products/$locationProductId/deactivate")
    check(response.statusCode == 200) { "Failed to deactivate '$productName': ${response.asString()}" }
  }

  @Then("the location product named {string} should have {string} on hand")
  fun verifyOnHand(productName: String, expectedQuantity: String) {
    AsyncPosting.untilAsserted("stock on hand of $productName") {
      val actualQuantity = BigDecimal(locationProductNamed(productName).path("stockBalance").asText())
      val mismatchMessage = "on hand of $productName was $actualQuantity"
      assertEquals(0, BigDecimal(expectedQuantity).compareTo(actualQuantity), mismatchMessage)
    }
  }

  private fun locationProductNamed(productName: String): JsonNode {
    val searchBody = PageRequest(
      previousCursor = "",
      requestedSize = 50,
      parameters = ProductSearchParameters(statusList = ProductStatus.entries.toSet())
    )
    val response = apiClient.post("/secured/location-products/search", searchBody)
    check(response.statusCode == 200) { "location product search answered ${response.statusCode}" }
    val matches = objectMapper.readTree(response.asString()).path("contents")
      .filter { it.path("productName").asText() == productName }
    assertEquals(1, matches.size, "location products named '$productName' in the catalog")
    return matches.single()
  }
}
