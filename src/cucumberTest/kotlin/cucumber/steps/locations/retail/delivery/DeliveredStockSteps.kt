package app.myliberty.easy_records.cucumber.steps.locations.retail.delivery

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.AsyncPosting
import app.myliberty.easy_records.cucumber.support.PurchaseRequests
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals

// Each row is its own order and delivery, so every row leaves its own FIFO layer for later sale scenarios.
class DeliveredStockSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext,
  private val purchaseRequests: PurchaseRequests
) {

  private val objectMapper = jacksonObjectMapper()

  @Given("stock of {string} is delivered by {string} in layers:")
  fun deliverStockInLayers(productName: String, supplierName: String, layersTable: DataTable) {
    val locationProductId = injectContext.getNamed(TransientKey.LOCATION_PRODUCT, productName)
    val supplierId = injectContext.getNamed(TransientKey.SUPPLIER, supplierName)
    layersTable.asMaps().forEach { layerRow ->
      val unitId = purchaseRequests.unitIdOf(layerRow)
      val quantity = BigDecimal(layerRow.getValue("quantity"))
      val unitCost = BigDecimal(layerRow.getValue("unit cost"))
      val orderedPurchase = orderLayer(supplierId, locationProductId, quantity, unitId, unitCost)
      deliverLayer(orderedPurchase, quantity, unitId, unitCost)
    }
  }

  private fun orderLayer(
    supplierId: UUID,
    locationProductId: UUID,
    quantity: BigDecimal,
    unitId: UUID,
    unitCost: BigDecimal
  ): OrderedPurchase {
    val orderRequest = purchaseRequests.orderRequest(
      supplierId, listOf(purchaseRequests.orderLine(locationProductId, quantity, unitId, unitCost))
    )
    val orderResponse = apiClient.post("/secured/purchases/order", orderRequest)
    check(orderResponse.statusCode == 200) { "Failed to order stock: ${orderResponse.asString()}" }
    val orderJson = orderResponse.jsonPath()
    return OrderedPurchase(
      purchaseId = UUID.fromString(orderJson.getString("id")),
      purchaseReference = orderJson.getString("referenceNumber"),
      purchaseLineId = UUID.fromString(orderJson.getString("lines[0].id"))
    )
  }

  private fun deliverLayer(orderedPurchase: OrderedPurchase, quantity: BigDecimal, unitId: UUID, unitCost: BigDecimal) {
    val deliveryRequest = purchaseRequests.deliveryRequest(
      orderedPurchase.purchaseId,
      "{{now}}",
      listOf(purchaseRequests.deliveryLine(orderedPurchase.purchaseLineId, quantity, unitId, unitCost))
    )
    val deliveryResponse = apiClient.post("/secured/deliveries", deliveryRequest)
    check(deliveryResponse.statusCode == 200) { "Failed to deliver stock: ${deliveryResponse.asString()}" }
    val deliveryReference = deliveryResponse.jsonPath().getList<String>("deliveries.referenceNumber").last()
    injectContext.store(TransientKey.PURCHASE, orderedPurchase.purchaseId)
    injectContext.storeText(TransientKey.PURCHASE_REFERENCE, orderedPurchase.purchaseReference)
    injectContext.storeText(TransientKey.DELIVERY_REFERENCE, deliveryReference)
    awaitReceived(orderedPurchase.purchaseReference, deliveryReference)
  }

  // A delivery turns RECEIVED in the same transaction that writes its stock layer, so a later layer queues behind it.
  private fun awaitReceived(purchaseReference: String, deliveryReference: String) {
    AsyncPosting.untilAsserted("delivery $deliveryReference of $purchaseReference received") {
      val purchasesResponse = apiClient.get("/secured/purchases", queryParams = mapOf("n" to 50))
      check(purchasesResponse.statusCode == 200) { "GET /secured/purchases answered ${purchasesResponse.statusCode}" }
      val purchase = objectMapper.readTree(purchasesResponse.asString())
        .first { it.path("referenceNumber").asText() == purchaseReference }
      val delivery = purchase.path("deliveries").first { it.path("referenceNumber").asText() == deliveryReference }
      assertEquals("RECEIVED", delivery.path("status").asText(), "status of delivery $deliveryReference")
    }
  }

  private data class OrderedPurchase(val purchaseId: UUID, val purchaseReference: String, val purchaseLineId: UUID)
}
