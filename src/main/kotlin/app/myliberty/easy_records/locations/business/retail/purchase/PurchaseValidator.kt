package app.myliberty.easy_records.locations.business.retail.purchase

import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import java.math.BigDecimal

object PurchaseValidator {

  fun guardNoDuplicateProducts(lines: List<HasLocationProduct>) {
    val productIds = lines.map { it.locationProductId }
    if (productIds.size != productIds.toSet().size)
      throw LibertyGenericException("A purchase cannot have more than one line for the same product.")
  }

  fun guardHasLines(lines: List<*>) {
    if (lines.isEmpty())
      throw LibertyGenericException("An order must have at least one item.")
  }

  fun guardPositiveUnitCosts(unitCosts: List<BigDecimal>) {
    if (unitCosts.any { it.signum() <= 0 })
      throw LibertyGenericException("Line unit cost must be positive.")
  }

  fun guardIsDraft(purchase: PurchaseEntity) {
    if (purchase.purchaseStatus != PurchaseStatus.DRAFT)
      throw LibertyGenericException("Purchase is not in draft status anymore. The requested updates cannot be applied.")
  }

  fun guardCanCancelLines(purchase: PurchaseEntity) {
    if (purchase.purchaseStatus !in listOf(PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED))
      throw LibertyGenericException("Cannot cancel lines on a purchase with status ${purchase.purchaseStatus}")
  }

  fun guardCanReceiveDelivery(purchase: PurchaseEntity) {
    when (purchase.purchaseStatus) {
      PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED -> return
      PurchaseStatus.DRAFT ->
        throw LibertyGenericException("Cannot record a delivery on a draft purchase. Confirm the order first.")
      PurchaseStatus.FULLY_DELIVERED ->
        throw LibertyGenericException("Cannot record a delivery on a fully delivered purchase.")
      PurchaseStatus.CANCELED ->
        throw LibertyGenericException("Cannot record a delivery on a canceled purchase.")
    }
  }

  // Cancel is absolute: it replaces the counter, which plus delivered may not exceed ordered, in storage units
  fun guardCancelQuantity(line: PurchaseLineEntity, storageQuantityToCancel: ExactQuantity, quantityBreakdown: QuantityBreakdown) {
    val maxCancelable = line.storageGrossOrderedQty() - ExactQuantity.ofStorage(line.storageQuantityDelivered)
    if (storageQuantityToCancel > maxCancelable)
      throw LibertyGenericException(
        "Cannot cancel ${quantityBreakdown.render(storageQuantityToCancel)} of line ${line.requiredReference()}. " +
          "Ordered: ${quantityBreakdown.render(line.storageGrossOrderedQty())}, " +
          "Delivered: ${quantityBreakdown.render(ExactQuantity.ofStorage(line.storageQuantityDelivered))}, " +
          "Max cancelable: ${quantityBreakdown.render(maxCancelable)}"
      )
  }
}
