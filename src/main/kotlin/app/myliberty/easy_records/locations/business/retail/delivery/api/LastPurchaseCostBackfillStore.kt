package app.myliberty.easy_records.locations.business.retail.delivery.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.delivery.PurchaseDeliveryLineRepository
import app.myliberty.easy_records.locations.business.retail.location_product.api.LastPurchaseCostUpdateDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryStatus
import org.springframework.stereotype.Service
import java.util.UUID

// Short-lived: deleted with the rest of the last purchase cost backfill once every organization has run it.
@Service
@TransactionalOnLocationSchema(readOnly = true)
class LastPurchaseCostBackfillStore(
    private val purchaseDeliveryLineRepository: PurchaseDeliveryLineRepository,
) {

    fun findLastDeliveredCostsAfter(
        afterLocationProductId: UUID,
        batchSize: Int
    ): List<LastPurchaseCostUpdateDto> =
        purchaseDeliveryLineRepository.findLastDeliveredCostsAfter(
            PurchaseDeliveryStatus.RECEIVED.code,
            PurchaseDeliveryLineStatus.ACTIVE.code,
            afterLocationProductId,
            batchSize
        ).map {
            LastPurchaseCostUpdateDto(
                locationProductId = it.locationProductId,
                unitCost = it.unitCost,
                storageUnitsPerUserProvidedUnit = it.storageUnitsPerUserProvidedUnit,
            )
        }
}
