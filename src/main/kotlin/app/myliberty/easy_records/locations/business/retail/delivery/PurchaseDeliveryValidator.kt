package app.myliberty.easy_records.locations.business.retail.delivery

import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryCreateDto
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineDto
import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PurchaseDeliveryValidator(
  private val locationProductDataFetcher: LocationProductDataFetcher,
  private val productStockBreakdownProvider: ProductStockBreakdownProvider
) {

  fun validate(purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto, purchaseLineById: Map<UUID, PurchaseLineDto>) {
    if (purchaseDeliveryCreateDto.lines.isEmpty())
      throw LibertyGenericException("Delivery must have at least one line.")

    val lineIds = purchaseDeliveryCreateDto.lines.map { it.purchaseLineId }
    if (lineIds.size != lineIds.toSet().size)
      throw LibertyGenericException("Duplicate purchase line IDs in delivery request.")

    purchaseDeliveryCreateDto.lines.forEach { lineDto ->
      if (lineDto.purchaseLineId !in purchaseLineById)
        throw LibertyGenericException("Purchase line ${lineDto.purchaseLineId} does not belong to this purchase.")
    }
  }

  fun ensurePendingDeliveryQuantityIsNotExceeded(
    purchaseDeliveryCreateDto: PurchaseDeliveryCreateDto,
    purchaseLineById: Map<UUID, PurchaseLineDto>,
    storageUnitsPerUserProvidedUnitByPurchaseLineId: Map<UUID, Long>
  ) {
    val locationProducts = locationProductDataFetcher.findSummaryByIds(
      purchaseLineById.values.map { it.locationProductId }.toSet()
    )
    val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
      purchaseLineById.values.map { it.locationProductId }.toSet()
    )
    purchaseDeliveryCreateDto.lines.forEach { lineDto ->
      val purchaseLine = purchaseLineById.getValue(lineDto.purchaseLineId)
      val delivered = ExactQuantity.ofStorageFromUserProvided(
        lineDto.userProvidedQuantityDelivered,
        storageUnitsPerUserProvidedUnitByPurchaseLineId.getValue(lineDto.purchaseLineId)
      )
      if (delivered > purchaseLine.storagePendingDeliveryQty) {
        throw LibertyGenericException(
          maximumQuantityMessageOf(
            locationProducts.getValue(purchaseLine.locationProductId).label,
            breakdownByLocationProductId.getValue(purchaseLine.locationProductId),
            purchaseLine.storagePendingDeliveryQty,
            delivered
          )
        )
      }
    }
  }

  fun maximumQuantityProblem(
    documentReference: String,
    locationProductId: UUID,
    maximumQuantity: ExactQuantity,
    providedQuantity: ExactQuantity
  ): BlockingProblem? {
    if (providedQuantity <= maximumQuantity) return null
    val quantityBreakdown = productStockBreakdownProvider.forLocationProduct(locationProductId)
    return BlockingProblem(
      documentReference = documentReference,
      description = maximumQuantityMessageOf(
        locationProductDataFetcher.findSummaryByIds(setOf(locationProductId)).getValue(locationProductId).label,
        quantityBreakdown,
        maximumQuantity,
        providedQuantity
      ),
      effect = "Enter ${quantityBreakdown.render(maximumQuantity)} or less."
    )
  }

  private fun maximumQuantityMessageOf(
    locationProductLabel: String,
    quantityBreakdown: QuantityBreakdown,
    maximumQuantity: ExactQuantity,
    providedQuantity: ExactQuantity
  ): String {
    val maximumQuantityText = quantityBreakdown.render(maximumQuantity)
    val providedQuantityText = quantityBreakdown.render(providedQuantity)
    return "You can deliver at most $maximumQuantityText of '$locationProductLabel', " +
      "but you entered $providedQuantityText."
  }
}
