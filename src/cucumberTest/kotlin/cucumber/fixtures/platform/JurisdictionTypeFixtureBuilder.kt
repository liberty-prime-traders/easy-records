package app.myliberty.easy_records.cucumber.fixtures.platform

import app.myliberty.easy_records.cucumber.fixtures.FixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.platform.business.jurisdiction_type.api.JurisdictionTypeInsertDto
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JurisdictionTypeFixtureBuilder(
  injectContext: InjectContext,
  apiClient: ApiClient,
) : FixtureBuilder<JurisdictionTypeInsertDto>(injectContext, apiClient) {

  override val endpoint = "/secured/jurisdiction-types"

  private val objectMapper = jacksonObjectMapper()

  override fun defaultDto() = JurisdictionTypeInsertDto(name = "Jurisdiction Type")

  // Jurisdiction types survive the per-scenario reset, so a name already registered is reused rather than re-created.
  fun createNamed(name: String): UUID = findIdByName(name) ?: create(defaultDto().copy(name = name))

  private fun findIdByName(name: String): UUID? {
    val response = apiClient.get(endpoint)
    check(response.statusCode == 200) { "Failed to list jurisdiction types: ${response.asString()}" }
    return objectMapper.readTree(response.asString())
      .firstOrNull { it.path("name").asText() == name }
      ?.let { UUID.fromString(it.path("id").asText()) }
  }
}
