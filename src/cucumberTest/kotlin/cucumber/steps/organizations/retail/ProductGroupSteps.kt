package app.myliberty.easy_records.cucumber.steps.organizations.retail

import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.fixtures.organizations.retail.ProductGroupFixtureBuilder
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey

class ProductGroupSteps(
  private val productGroupFixtureBuilder: ProductGroupFixtureBuilder,
  private val injectContext: InjectContext
) {

  @Given("a product group exists")
  fun createProductGroup() {
    injectContext.store(TransientKey.PRODUCT_GROUP, productGroupFixtureBuilder.create())
  }

  @Given("a product group named {string} exists")
  fun createNamedProductGroup(name: String) {
    injectContext.store(TransientKey.PRODUCT_GROUP, productGroupFixtureBuilder.createNamed(name))
  }
}
