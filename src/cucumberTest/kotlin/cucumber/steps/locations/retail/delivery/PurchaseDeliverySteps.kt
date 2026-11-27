package app.myliberty.easy_records.cucumber.steps.locations.retail.delivery

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.PurchaseRequests
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import java.math.BigDecimal

class PurchaseDeliverySteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val purchaseRequests: PurchaseRequests
) {

  @Given("a delivery is recorded on the purchase with lines:")
  fun recordDelivery(deliveryLinesTable: DataTable) {
    recordDeliveryAt("{{now}}", deliveryLinesTable)
  }

  @Given("a delivery dated {string} is recorded on the purchase with lines:")
  fun recordDeliveryAt(deliveredAt: String, deliveryLinesTable: DataTable) {
    val deliveryLines = deliveryLinesTable.asMaps().map { row ->
      purchaseRequests.deliveryLine(
        purchaseLineId = injectContext.getNamed(TransientKey.PURCHASE_LINE, row.getValue("product")),
        quantity = BigDecimal(row.getValue("quantity")),
        unitId = purchaseRequests.unitIdOf(row),
        unitCost = BigDecimal(row.getValue("unit cost"))
      )
    }
    val deliveryRequest = purchaseRequests.deliveryRequest(
      injectContext.get(TransientKey.PURCHASE), deliveredAt, deliveryLines
    )
    val response = apiClient.post("/secured/deliveries", deliveryRequest)
    check(response.statusCode == 200) { "Failed to record delivery: ${response.asString()}" }
    val deliveryReferences = response.jsonPath().getList<String>("deliveries.referenceNumber")
    injectContext.storeText(TransientKey.DELIVERY_REFERENCE, deliveryReferences.last())
  }
}
