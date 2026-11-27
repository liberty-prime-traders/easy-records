package app.myliberty.easy_records.cucumber.steps.organizations.retail

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import app.myliberty.easy_records.cucumber.fixtures.organizations.retail.ProductFixtureBuilder
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey

class ProductSteps(
  private val injectContext: InjectContext,
  private val productFixtureBuilder: ProductFixtureBuilder
) {

  @Given("the following products exist:")
  fun createProducts(dataTable: DataTable) {
    dataTable.asMaps().forEach { row ->
      injectContext.store(TransientKey.PRODUCT, productFixtureBuilder.createFromRow(row))
    }
  }

  @Given("a product named {string} with base unit {string} exists")
  fun createNamedProductWithBaseUnit(name: String, unitCode: String) {
    val baseUnitId = injectContext.getNamed(TransientKey.UNIT_VALUE, unitCode)
    injectContext.store(TransientKey.PRODUCT, productFixtureBuilder.createNamedWithBaseUnit(name, baseUnitId))
  }

  @Given("a product named {string} exists")
  fun createNamedProduct(name: String) {
    injectContext.store(TransientKey.PRODUCT, productFixtureBuilder.createNamed(name))
  }
}
