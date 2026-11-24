package app.myliberty.easy_records.cucumber.steps.organizations.retail

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import app.myliberty.easy_records.cross_tier.retail.product.search.common.ProductSearchParameters
import app.myliberty.easy_records.cucumber.fixtures.organizations.retail.ProductFixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.util.paging.PageRequest
import app.myliberty.easy_records.util.queries.SearchStrategy

class ProductSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val productFixtureBuilder: ProductFixtureBuilder
) {

  @Given("the following products exist:")
  fun createProducts(dataTable: DataTable) {
    dataTable.asMaps().forEach { row ->
      injectContext.store(TransientKey.PRODUCT, productFixtureBuilder.createFromRow(row))
    }
  }

  @Given("a product named {string} exists")
  fun createNamedProduct(name: String) {
    injectContext.store(TransientKey.PRODUCT, productFixtureBuilder.createNamed(name))
  }

  @When("I create a product with name {string} and description {string}")
  fun createProduct(name: String, description: String) {
    injectContext.store(
      TransientKey.PRODUCT,
      productFixtureBuilder.createFromRow(mapOf("productName" to name, "description" to description))
    )
  }

  @When("I search for products with text {string}")
  fun searchProducts(searchText: String) {
    apiClient.post(
      "/secured/products/search",
      PageRequest(
        previousCursor = null,
        requestedSize = 50,
        parameters = ProductSearchParameters(searchText = searchText, searchStrategy = SearchStrategy.FULLTEXT)
      )
    )
  }
}
