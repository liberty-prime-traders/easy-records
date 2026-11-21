package app.myliberty.easy_records.cucumber.fixtures.organizations

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.retail.product_group.api.ProductGroupInsertDto
import org.springframework.stereotype.Component

@Component
class ProductGroupFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<ProductGroupInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/product-groups"

  override fun defaultDto() = ProductGroupInsertDto(
    groupName = "Test Group",
    categoryId = injectContext.get(TransientKey.CATEGORY)
  )
}
