package app.myliberty.easy_records.cucumber.steps.locations

import io.cucumber.java.en.Then
import app.myliberty.easy_records.cross_tier.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductResponseDto
import app.myliberty.easy_records.support.TestConstants
import app.myliberty.easy_records.util.paging.PageRequest
import org.awaitility.Awaitility.await
import java.time.Duration

class CatalogSyncSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  @Then("the location catalog should contain product {string}")
  fun verifyLocationCatalogContainsProduct(productName: String) {
    val orgProductId = injectContext.get(TransientKey.PRODUCT)

    val searchBody = PageRequest(
      previousCursor = null,
      requestedSize = 50,
      parameters = ProductSearchParameters(searchText = productName)
    )

    await().alias("Product '$productName' (id=$orgProductId) not found in location catalog")
      .atMost(Duration.ofMillis(TestConstants.Timeouts.KAFKA_SYNC_MS))
      .pollInterval(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
      .until {
        apiClient.post("/secured/location-products/search", searchBody)
          .jsonPath()
          .getList("contents", LocationProductResponseDto::class.java)
          ?.any { it.productName == productName && it.orgProductId == orgProductId } == true
      }
  }
}
