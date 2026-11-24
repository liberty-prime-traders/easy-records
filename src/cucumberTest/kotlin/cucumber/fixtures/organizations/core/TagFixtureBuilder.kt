package app.myliberty.easy_records.cucumber.fixtures.organizations.core

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.core.tag.api.CategoryType
import app.myliberty.easy_records.organizations.business.core.tag.api.TagInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TagFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<TagInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/tag"

  override fun defaultDto() = TagInsertDto(category = CategoryType.PRODUCT, tagName = "Test Tag")

  fun createNamed(name: String): UUID = create(defaultDto().copy(tagName = name))

  fun createNamedInCategory(name: String, category: CategoryType): UUID =
    create(defaultDto().copy(tagName = name, category = category))
}
