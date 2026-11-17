package app.myliberty.easy_records.locations.business.retail.delivery

import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineDto
import app.myliberty.easy_records.locations.business.retail.stock.api.read.DeliveryLineStockInspector
import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Component

@Component
class PurchaseDeliveryRevisionValidator(
  private val purchaseDeliveryValidator: PurchaseDeliveryValidator,
  private val deliveryLineStockInspector: DeliveryLineStockInspector
) {

  fun problemIfLineIsNotActive(purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity): BlockingProblem? {
    if (purchaseDeliveryLineEntity.status == PurchaseDeliveryLineStatus.ACTIVE) return null
    return BlockingProblem(
      documentReference = purchaseDeliveryLineEntity.requiredReference(),
      description = "This delivery line has already been changed.",
      effect = "Change its latest line instead."
    )
  }

  fun problemIfPendingDeliveryQuantityIsExceeded(
    purchaseLineDto: PurchaseLineDto,
    revisedLine: PurchaseDeliveryLineEntity,
    replacementStorageQuantity: ExactQuantity
  ): BlockingProblem? {
    val revisedStorageQuantity = ExactQuantity.ofStorageFromUserProvided(
      revisedLine.userProvidedQuantityDelivered,
      revisedLine.storageUnitsPerUserProvidedUnit
    )
    val netRemaining = purchaseLineDto.storageQuantityRemaining + revisedStorageQuantity
    return purchaseDeliveryValidator.maximumQuantityProblem(
      revisedLine.requiredReference(), purchaseLineDto.locationProductId, netRemaining, replacementStorageQuantity
    )
  }

  fun problemIfStockWasTransferred(
    deliveryReferenceNumber: String,
    purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity
  ): BlockingProblem? {
    if (!deliveryLineStockInspector.hasTransferredOut(
        deliveryReferenceNumber, purchaseDeliveryLineEntity.requiredReference())
    ) return null
    return BlockingProblem(
      documentReference = purchaseDeliveryLineEntity.requiredReference(),
      description = "Some of this delivery's stock has already been sent to another branch.",
      effect = "This line cannot be changed."
    )
  }

  fun problemIfStockHasMovementsThatCannotBeReversed(
    deliveryReferenceNumber: String,
    purchaseDeliveryLineEntity: PurchaseDeliveryLineEntity
  ): BlockingProblem? {
    val hasIrreversibleMovements = deliveryLineStockInspector.hasMovementsThatCannotBeReversed(
      deliveryReferenceNumber, purchaseDeliveryLineEntity.requiredReference()
    )
    if (hasIrreversibleMovements) {
      return BlockingProblem(
        documentReference = purchaseDeliveryLineEntity.requiredReference(),
        description = "Some of this delivery's stock was returned, and returns cannot be undone.",
        effect = "This line cannot be changed."
      )
    }
    return null
  }

  fun problemsOf(
    deliveryReferenceNumber: String,
    purchaseLineDto: PurchaseLineDto,
    revisedLine: PurchaseDeliveryLineEntity,
    replacementStorageQuantity: ExactQuantity
  ): List<BlockingProblem> = listOfNotNull(
    problemIfLineIsNotActive(revisedLine),
    problemIfPendingDeliveryQuantityIsExceeded(purchaseLineDto, revisedLine, replacementStorageQuantity),
    problemIfStockWasTransferred(deliveryReferenceNumber, revisedLine),
    problemIfStockHasMovementsThatCannotBeReversed(deliveryReferenceNumber, revisedLine)
  )
}
