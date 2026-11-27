package app.myliberty.easy_records.cucumber.support

import app.myliberty.easy_records.cucumber.support.context.InjectContext
import app.myliberty.easy_records.cucumber.support.context.TransientKey
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

// The request shapes for ordering and delivering, shared by every step that builds a purchase for a scenario.
@Component
class PurchaseRequests(
  private val injectContext: InjectContext
) {

  fun unitIdOf(tableRow: Map<String, String>): UUID {
    val unitCode = tableRow["unit"] ?: return injectContext.get(TransientKey.UNIT_VALUE)
    return injectContext.getNamed(TransientKey.UNIT_VALUE, unitCode)
  }

  fun orderRequest(supplierId: UUID, orderLines: List<Map<String, Any>>): Map<String, Any> =
    mapOf("supplierId" to supplierId.toString(), "linesToAdd" to orderLines)

  fun orderLine(locationProductId: UUID, quantity: BigDecimal, unitId: UUID, unitCost: BigDecimal): Map<String, Any> =
    mapOf(
      "locationProductId" to locationProductId.toString(),
      "quantityOrdered" to quantity,
      "unitId" to unitId.toString(),
      "unitCost" to unitCost
    )

  fun deliveryRequest(purchaseId: UUID, deliveredAt: String, deliveryLines: List<Map<String, Any>>): Map<String, Any> =
    mapOf(
      "purchaseId" to purchaseId.toString(),
      "deliveredAt" to injectContext.inject(deliveredAt),
      "lines" to deliveryLines
    )

  fun deliveryLine(purchaseLineId: UUID, quantity: BigDecimal, unitId: UUID, unitCost: BigDecimal): Map<String, Any> =
    mapOf(
      "purchaseLineId" to purchaseLineId.toString(),
      "quantityDelivered" to quantity,
      "unitId" to unitId.toString(),
      "unitCost" to unitCost
    )
}
