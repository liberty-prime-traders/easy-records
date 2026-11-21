package app.myliberty.easy_records.cucumber.fixtures.organizations

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupInsertDto
import org.springframework.stereotype.Component

@Component
class UnitGroupFixtureBuilder(
    injectContext: InjectContext,
    apiClient: ApiClient
) : FixtureBuilder<UnitGroupInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/unitgroups"

  override fun defaultDto() = UnitGroupInsertDto(name = "Test Unit Group")
}
