package app.myliberty.easy_records.cucumber.steps.organizations

import app.myliberty.easy_records.cucumber.fixtures.organizations.UnitGroupFixtureBuilder
import app.myliberty.easy_records.cucumber.fixtures.organizations.UnitValueFixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import io.cucumber.java.en.Given
import java.util.UUID

class UnitSteps(
  private val unitGroupFixtureBuilder: UnitGroupFixtureBuilder,
  private val unitValueFixtureBuilder: UnitValueFixtureBuilder,
  private val injectContext: InjectContext,
  private val apiClient: ApiClient
) {

  @Given("a unit group exists")
  fun createUnitGroup() {
    storeUnitGroup(unitGroupFixtureBuilder.create())
  }

  @Given("a unit group named {string} exists")
  fun createNamedUnitGroup(name: String) {
    storeUnitGroup(unitGroupFixtureBuilder.createNamed(name))
  }

  @Given("the system unit group {string} is selected")
  fun selectSystemUnitGroup(name: String) {
    val response = apiClient.get("/secured/unitgroups")
    val systemUnitGroupId = response.jsonPath().getString("find { it.name == '$name' && it.systemDefined }.id")
    storeUnitGroup(UUID.fromString(checkNotNull(systemUnitGroupId) { "No system unit group named '$name'" }))
  }

  @Given("the system unit {string} is selected")
  fun selectSystemUnit(code: String) {
    val response = apiClient.get("/secured/unitvalues")
    val systemUnitId = response.jsonPath().getString("find { it.code == '$code' && it.systemDefined }.id")
    injectContext.store(TransientKey.UNIT_VALUE, UUID.fromString(checkNotNull(systemUnitId) { "No system unit coded '$code'" }))
  }

  @Given("a unit exists")
  fun createUnit() {
    injectContext.store(TransientKey.UNIT_VALUE, unitValueFixtureBuilder.create())
  }

  @Given("a unit named {string} coded {string} exists")
  fun createNamedUnit(name: String, code: String) {
    injectContext.store(TransientKey.UNIT_VALUE, unitValueFixtureBuilder.createNamed(name, code))
  }

  private fun storeUnitGroup(unitGroupId: UUID) {
    injectContext.store(TransientKey.UNIT_GROUP, unitGroupId)
    injectContext.store(TransientKey.ROOT_UNIT, unitGroupFixtureBuilder.rootUnitIdOf(unitGroupId))
  }
}
