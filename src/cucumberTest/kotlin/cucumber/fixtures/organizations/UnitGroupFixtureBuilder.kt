package app.myliberty.easy_records.cucumber.fixtures.organizations

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupInsertDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnitGroupFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient
) : FixtureBuilder<UnitGroupInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/unitgroups"

  override fun defaultDto() = UnitGroupInsertDto(name = "Test Unit Group")

  fun createNamed(name: String): UUID = create(defaultDto().copy(name = name))

  fun rootUnitIdOf(unitGroupId: UUID): UUID {
    val response = apiClient.get("/secured/unitvalues", mapOf("unitGroupId" to unitGroupId))
    check(response.statusCode == 200) { "Failed to list units of group $unitGroupId: ${response.asString()}" }
    return UUID.fromString(response.jsonPath().getString("find { it.baseUnit == null }.id"))
  }
}
