package app.myliberty.easy_records.cucumber.steps.organizations.retail

import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.fixtures.organizations.retail.CategoryFixtureBuilder
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey

class CategorySteps(
  private val categoryFixtureBuilder: CategoryFixtureBuilder,
  private val injectContext: InjectContext
) {

  @Given("a category exists")
  fun createCategory() {
    injectContext.store(TransientKey.CATEGORY, categoryFixtureBuilder.create())
  }

  @Given("a category named {string} exists")
  fun createNamedCategory(name: String) {
    injectContext.store(TransientKey.CATEGORY, categoryFixtureBuilder.createNamed(name))
  }
}
