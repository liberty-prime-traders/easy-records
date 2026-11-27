package app.myliberty.easy_records.cucumber.steps.locations.retail.delivery

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import java.math.BigDecimal
import java.util.UUID

class PurchaseDeliverySteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  @Given("a delivery is recorded on the purchase with lines:")
  fun recordDelivery(deliveryLinesTable: DataTable) {
    recordDeliveryAt("{{now}}", deliveryLinesTable)
  }

  @Given("a delivery dated {string} is recorded on the purchase with lines:")
  fun recordDeliveryAt(deliveredAt: String, deliveryLinesTable: DataTable) {
    val deliveryRequest = mapOf(
      "purchaseId" to injectContext.get(TransientKey.PURCHASE).toString(),
      "deliveredAt" to injectContext.inject(deliveredAt),
      "lines" to deliveryLinesTable.asMaps().map { row ->
        mapOf(
          "purchaseLineId" to injectContext.getNamed(TransientKey.PURCHASE_LINE, row.getValue("product")).toString(),
          "quantityDelivered" to BigDecimal(row.getValue("quantity")),
          "unitId" to unitIdOf(row).toString(),
          "unitCost" to BigDecimal(row.getValue("unit cost"))
        )
      }
    )
    val response = apiClient.post("/secured/deliveries", deliveryRequest)
    check(response.statusCode == 200) { "Failed to record delivery: ${response.asString()}" }
    val deliveryReferences = response.jsonPath().getList<String>("deliveries.referenceNumber")
    injectContext.storeText(TransientKey.DELIVERY_REFERENCE, deliveryReferences.last())
  }

  private fun unitIdOf(row: Map<String, String>): UUID {
    val unitCode = row["unit"] ?: return injectContext.get(TransientKey.UNIT_VALUE)
    return injectContext.getNamed(TransientKey.UNIT_VALUE, unitCode)
  }
}
