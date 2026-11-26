package app.myliberty.easy_records.cucumber.steps.locations.retail.purchase

import app.myliberty.easy_records.cucumber.support.ApiClient
import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.ResponseContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import io.restassured.response.Response
import java.math.BigDecimal
import java.util.UUID

class PurchaseSteps(
  private val apiClient: ApiClient,
  private val injectContext: InjectContext
) {

  @Given("a draft purchase from {string} with lines:")
  fun createDraft(supplierName: String, linesTable: DataTable) =
    createPurchase("/secured/purchases/draft", supplierName, linesTable)

  @Given("an order from {string} with lines:")
  fun createOrder(supplierName: String, linesTable: DataTable) =
    createPurchase("/secured/purchases/order", supplierName, linesTable)

  @Given("the following quantities are canceled on the purchase:")
  fun cancelLines(cancelTable: DataTable) {
    val purchaseId = injectContext.get(TransientKey.PURCHASE)
    val cancelRequests = cancelTable.asMaps().map { row ->
      mapOf(
        "purchaseLineId" to injectContext.getNamed(TransientKey.PURCHASE_LINE, row.getValue("product")).toString(),
        "quantityCanceled" to BigDecimal(row.getValue("quantity"))
      )
    }
    val response = apiClient.put("/secured/purchases/$purchaseId/line-cancel-quantities", cancelRequests)
    check(response.statusCode == 200) { "Failed to cancel purchase lines: ${response.asString()}" }
  }

  private fun createPurchase(path: String, supplierName: String, linesTable: DataTable) {
    val purchaseRequest = mapOf(
      "supplierId" to injectContext.getNamed(TransientKey.SUPPLIER, supplierName).toString(),
      "linesToAdd" to linesTable.asMaps().map { row ->
        val locationProductId = injectContext.getNamed(TransientKey.LOCATION_PRODUCT, row.getValue("product"))
        mapOf(
          "locationProductId" to locationProductId.toString(),
          "quantityOrdered" to BigDecimal(row.getValue("quantity")),
          "unitId" to unitIdOf(row).toString(),
          "unitCost" to BigDecimal(row.getValue("unit cost"))
        )
      }
    )
    val response = apiClient.post(path, purchaseRequest)
    check(response.statusCode == 200) { "Failed to create purchase at $path: ${response.asString()}" }
    rememberPurchase(response)
  }

  private fun unitIdOf(row: Map<String, String>): UUID {
    val unitCode = row["unit"] ?: return injectContext.get(TransientKey.UNIT_VALUE)
    return injectContext.getNamed(TransientKey.UNIT_VALUE, unitCode)
  }

  private fun rememberPurchase(response: Response) {
    injectContext.store(TransientKey.PURCHASE, ResponseContext.idFromResponse(response))
    injectContext.storeText(TransientKey.PURCHASE_REFERENCE, response.jsonPath().getString("referenceNumber"))
    val lineProductNames = response.jsonPath().getList<String>("lines.locationProduct.productName")
    lineProductNames.forEachIndexed { lineIndex, productName ->
      val lineJson = "lines[$lineIndex]"
      injectContext.storeNamedText(
        TransientKey.PURCHASE_LINE, productName, response.jsonPath().getString("$lineJson.id")
      )
      injectContext.storeNamedText(
        TransientKey.PURCHASE_LINE_REFERENCE, productName, response.jsonPath().getString("$lineJson.referenceNumber")
      )
    }
  }
}
