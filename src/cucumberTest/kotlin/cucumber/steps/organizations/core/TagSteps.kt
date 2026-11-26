package app.myliberty.easy_records.cucumber.steps.organizations.core

import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.fixtures.organizations.core.TagFixtureBuilder
import app.myliberty.easy_records.organizations.business.core.tag.api.CategoryType

class TagSteps(
  private val tagFixtureBuilder: TagFixtureBuilder
) {

  @Given("a tag named {string} exists")
  fun createNamedTag(name: String) {
    tagFixtureBuilder.createNamed(name)
  }

  @Given("an expense tag named {string} exists")
  fun createNamedExpenseTag(name: String) {
    tagFixtureBuilder.createNamedInCategory(name, CategoryType.EXPENSE)
  }
}
