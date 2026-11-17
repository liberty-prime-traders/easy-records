package app.myliberty.easy_records.cucumber.fixtures.organizations

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.DtoConverter
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.product.api.OrganizationProductInsertDto
import org.springframework.stereotype.Component

@Component
class ProductFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient,
  private val dtoConverter: DtoConverter,
) : FixtureBuilder<OrganizationProductInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/products"

  override fun defaultDto() = OrganizationProductInsertDto(
    productName = "Test Product",
    productGroupId = injectContext.get(TransientKey.PRODUCT_GROUP),
    baseUnitId = injectContext.get(TransientKey.UNIT_VALUE),
  )

  override fun fromRow(row: Map<String, String>) =
    dtoConverter.fromRow(row, defaultDto(), OrganizationProductInsertDto::class.java)
}
