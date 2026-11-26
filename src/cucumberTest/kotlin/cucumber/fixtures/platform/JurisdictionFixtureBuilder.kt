package app.myliberty.easy_records.cucumber.fixtures.platform

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.platform.business.jurisdiction.api.JurisdictionInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JurisdictionFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient,
) : FixtureBuilder<JurisdictionInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/jurisdictions"

  override fun defaultDto() = JurisdictionInsertDto(
    name = "Jurisdiction",
    jurisdictionTypeId = injectContext.get(TransientKey.JURISDICTION_TYPE),
  )

  fun createNamed(name: String, parentJurisdictionId: UUID? = null): UUID =
    create(defaultDto().copy(name = name, parentJurisdictionId = parentJurisdictionId))
}
