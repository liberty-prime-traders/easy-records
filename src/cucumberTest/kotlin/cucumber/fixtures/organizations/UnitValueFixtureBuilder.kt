package app.myliberty.easy_records.cucumber.fixtures.organizations

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueInsertDto
import org.springframework.stereotype.Component

@Component
class UnitValueFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<UnitValueInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/unitvalues"

  override fun defaultDto() = UnitValueInsertDto(
    name = "Test Unit",
    code = "TU",
    unitGroupId = injectContext.get(TransientKey.UNIT_GROUP)
  )
}
