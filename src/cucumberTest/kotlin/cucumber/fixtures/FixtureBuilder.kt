package app.myliberty.easy_records.cucumber.fixtures

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import java.util.UUID

abstract class FixtureBuilder<INSERT_DTO>(
  protected val injectContext: InjectContext,
  protected val apiClient: ApiClient,
) {

  protected abstract val endpoint: String
  protected abstract fun defaultDto(): INSERT_DTO

  protected open fun fromRow(row: Map<String, String>): INSERT_DTO = defaultDto()

  fun create(dto: INSERT_DTO? = null): UUID {
    val resolvedDto = try {
      dto ?: defaultDto()
    } catch (e: IllegalStateException) {
      throw IllegalStateException("${this::class.simpleName} prerequisite missing: ${e.message}", e)
    }
    val response = apiClient.post(endpoint, resolvedDto)
    check(response.statusCode == 200) { "Failed to create fixture at $endpoint: ${response.asString()}" }
    return ResponseContext.idFromResponse(response)
  }

  fun createFromRow(row: Map<String, String>) = create(fromRow(row))
}
