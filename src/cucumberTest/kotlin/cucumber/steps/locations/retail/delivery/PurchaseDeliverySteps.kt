package app.myliberty.easy_records.cucumber.steps.locations.retail.delivery

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.PurchaseRequests
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import java.math.BigDecimal
import java.util.UUID

class PurchaseDeliverySteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val purchaseRequests: PurchaseRequests
) {

  private val objectMapper = jacksonObjectMapper()

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

  // A revision request names the delivery line by id, which only the purchase response carries.
  @Given("the delivery line of {string} on delivery {string} is remembered")
  fun rememberDeliveryLine(productName: String, deliveryReference: String) {
    val injectedDeliveryReference = injectContext.inject(deliveryReference)
    val purchasesResponse = apiClient.get("/secured/purchases", queryParams = mapOf("n" to 50))
    check(purchasesResponse.statusCode == 200) { "GET /secured/purchases answered ${purchasesResponse.asString()}" }
    val deliveryNode = objectMapper.readTree(purchasesResponse.asString())
      .flatMap { purchaseNode -> purchaseNode.path("deliveries") }
      .first { candidateNode -> candidateNode.path("referenceNumber").asText() == injectedDeliveryReference }
    val deliveryLineNode = deliveryNode.path("lines")
      .first { lineNode -> lineNode.path("locationProduct").path("productName").asText() == productName }
    injectContext.storeNamed(
      TransientKey.DELIVERY_LINE, productName, UUID.fromString(deliveryLineNode.path("id").asText())
    )
  }

}
