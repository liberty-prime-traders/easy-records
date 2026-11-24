package app.myliberty.easy_records.cucumber.steps.locations

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.AuthContext
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.PersistentKey
import app.myliberty.easy_records.cucumber.support.rest.JsonSubsetMatcher
import app.myliberty.easy_records.cucumber.support.rest.SubsetOptions
import app.myliberty.easy_records.organizations.business.core.location.api.LocationInsertDto
import app.myliberty.easy_records.organizations.business.core.location.api.LocationType
import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import app.myliberty.easy_records.support.TestConstants
import app.myliberty.easy_records.util.paging.PageRequest
import org.awaitility.Awaitility.await
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals

class CatalogSyncSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val authContext: AuthContext
) {
  private val objectMapper = jacksonObjectMapper()

  // Locations cannot be deleted and the reset keeps them, so the second one is created once and then found by name.
  @Given("a location named {string} exists")
  fun ensureLocationExists(locationName: String) {
    if (findLocationId(locationName) != null) return
    authContext.authenticateAsPlatformAdmin()
    try {
      val response = apiClient.post("/secured/locations", LocationInsertDto(locationType = LocationType.SHOP, name = locationName))
      check(response.statusCode == 200) { "Failed to create location '$locationName': ${response.asString()}" }
    } finally {
      authContext.initialize()
    }
  }

  @Then("the catalog of location {string} should contain a product matching:")
  fun verifyOtherLocationCatalogContainsProduct(locationName: String, expectedJson: String) {
    val locationId = checkNotNull(findLocationId(locationName)) { "No location named '$locationName'" }
    val ownLocationId = injectContext.find(PersistentKey.LOCATION)
    injectContext.persist(PersistentKey.LOCATION, locationId)
    try {
      verifyLocationCatalogContainsProduct(expectedJson)
    } finally {
      ownLocationId?.let { injectContext.persist(PersistentKey.LOCATION, it) }
    }
  }

  @Then("the location catalog should contain a product matching:")
  fun verifyLocationCatalogContainsProduct(expectedJson: String) {
    val expected = objectMapper.readTree(injectContext.inject(expectedJson))

    await().alias("No location catalog product matched $expected")
      .atMost(Duration.ofMillis(TestConstants.Timeouts.KAFKA_SYNC_MS))
      .pollInterval(Duration.ofMillis(TestConstants.Timeouts.KAFKA_POLL_INTERVAL_MS))
      .until {
        locationCatalogProducts().any { JsonSubsetMatcher.isJsonSubset(expected, it, SubsetOptions()) }
      }
  }

  @Then("the location catalog should hold {int} product(s)")
  fun verifyLocationCatalogSize(expectedCount: Int) {
    assertEquals(expectedCount, locationCatalogProducts().size)
  }

  private fun locationCatalogProducts(): List<JsonNode> {
    val searchBody = PageRequest(
      previousCursor = "",
      requestedSize = 50,
      parameters = ProductSearchParameters(statusList = ProductStatus.entries.toSet())
    )
    val body = objectMapper.readTree(apiClient.post("/secured/location-products/search", searchBody).asString())
    return body.path("contents").toList()
  }

  // The reset removes organization membership, so the organization user cannot list locations; the platform endpoint can.
  private fun findLocationId(locationName: String): UUID? {
    val organizationId = injectContext.get(PersistentKey.ORGANIZATION)
    authContext.authenticateAsPlatformAdmin()
    val response = try {
      apiClient.get("/secured/organizations/$organizationId/locations")
    } finally {
      authContext.initialize()
    }
    check(response.statusCode == 200) { "GET locations of organization answered ${response.statusCode}: '${response.asString()}'" }
    return objectMapper.readTree(response.asString())
      .firstOrNull { it.path("name").asText() == locationName }
      ?.path("id")?.asText()?.let(UUID::fromString)
  }
}
