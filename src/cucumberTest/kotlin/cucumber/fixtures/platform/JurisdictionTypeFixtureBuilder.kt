package app.myliberty.easy_records.cucumber.fixtures.platform

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JurisdictionTypeFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient,
) : FixtureBuilder<JurisdictionTypeInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/jurisdiction-types"

  override fun defaultDto() = JurisdictionTypeInsertDto(name = "Jurisdiction Type")

  fun createNamed(name: String): UUID = create(defaultDto().copy(name = name))
}
