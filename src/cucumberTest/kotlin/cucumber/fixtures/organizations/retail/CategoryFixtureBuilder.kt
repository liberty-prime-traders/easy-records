package app.myliberty.easy_records.cucumber.fixtures.organizations.retail

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.retail.product_category.api.ProductCategoryInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CategoryFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<ProductCategoryInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/product-category"

  override fun defaultDto() = ProductCategoryInsertDto(categoryName = "Test Category")

  fun createNamed(name: String): UUID = create(defaultDto().copy(categoryName = name))
}
