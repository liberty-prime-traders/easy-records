package app.myliberty.easy_records.cucumber.steps.organizations

import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.fixtures.organizations.ProductGroupFixtureBuilder
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
}
