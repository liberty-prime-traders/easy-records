package app.myliberty.easy_records.cucumber.steps.organizations.retail.unit

import app.myliberty.easy_records.cucumber.fixtures.organizations.retail.unit.UnitGroupFixtureBuilder
import app.myliberty.easy_records.cucumber.fixtures.organizations.retail.unit.UnitValueFixtureBuilder
import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupInsertDto
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueInsertDto
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

  @Given("a unit group named {string} with base unit {string} coded {string} exists")
  fun createUnitGroupWithBaseUnit(groupName: String, baseUnitName: String, baseUnitCode: String) {
    val unitGroupInsertDto = UnitGroupInsertDto(
      name = groupName,
      baseUnitName = baseUnitName,
      baseUnitCode = baseUnitCode
    )
    storeUnitGroup(unitGroupFixtureBuilder.create(unitGroupInsertDto))
    rememberUnit(injectContext.get(TransientKey.ROOT_UNIT), baseUnitCode)
  }

  @Given("a unit named {string} coded {string} equal to {int} of unit {string} exists")
  fun createUnitInChain(name: String, code: String, unitsOfBasePerUnit: Int, baseUnitCode: String) {
    val unitValueInsertDto = UnitValueInsertDto(
      name = name,
      code = code,
      unitGroupId = injectContext.get(TransientKey.UNIT_GROUP),
      baseUnit = injectContext.getNamed(TransientKey.UNIT_VALUE, baseUnitCode),
      unitsOfBasePerUnit = unitsOfBasePerUnit.toLong()
    )
    rememberUnit(unitValueFixtureBuilder.create(unitValueInsertDto), code)
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
    val selectedUnitId = UUID.fromString(checkNotNull(systemUnitId) { "No system unit coded '$code'" })
    injectContext.store(TransientKey.UNIT_VALUE, selectedUnitId)
    injectContext.storeNamed(TransientKey.UNIT_VALUE, code, selectedUnitId)
  }

  @Given("a unit exists")
  fun createUnit() {
    injectContext.store(TransientKey.UNIT_VALUE, unitValueFixtureBuilder.create())
  }

  @Given("a unit named {string} coded {string} exists")
  fun createNamedUnit(name: String, code: String) {
    injectContext.store(TransientKey.UNIT_VALUE, unitValueFixtureBuilder.createNamed(name, code))
  }

  private fun rememberUnit(unitId: UUID, code: String) {
    injectContext.store(TransientKey.UNIT_VALUE, unitId)
    injectContext.storeNamed(TransientKey.UNIT_VALUE, code, unitId)
  }

  private fun storeUnitGroup(unitGroupId: UUID) {
    injectContext.store(TransientKey.UNIT_GROUP, unitGroupId)
    injectContext.store(TransientKey.ROOT_UNIT, unitGroupFixtureBuilder.rootUnitIdOf(unitGroupId))
  }
}
