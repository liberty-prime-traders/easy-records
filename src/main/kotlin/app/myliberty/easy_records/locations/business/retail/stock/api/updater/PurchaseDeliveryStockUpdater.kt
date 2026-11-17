package app.myliberty.easy_records.locations.business.stock.api.updater

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.stock.InboundLayerSpec
import app.myliberty.easy_records.locations.business.stock.StockIncreaseRecorder
import app.myliberty.easy_records.locations.business.stock.api.StockMovementContexts
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import app.myliberty.easy_records.organizations.business.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class PurchaseDeliveryStockUpdater(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
) {

    fun recordPurchaseDelivery(event: PurchaseDeliveredEvent) {
        val layerSpecs = event.lines.map { line ->
            InboundLayerSpec(
                locationProductId = line.locationProductId,
                sourceType = StockItemSource.PURCHASE,
                movementType = MovementType.PURCHASE_RECEIVED,
                documentReference = event.deliveryReferenceNumber,
                detailReference = line.lineReferenceNumber,
                storageQuantity = ExactQuantity.ofStorageFromUserProvided(line.userProvidedQuantityDelivered, line.storageUnitsPerUserProvidedUnit),
                batchCost = Decimals.MoneyScale.multiply(line.unitCost, line.userProvidedQuantityDelivered),
                userProvidedQuantity = line.userProvidedQuantityDelivered,
                unitId = line.unitId,
                storageUnitsPerUserProvidedUnit = line.storageUnitsPerUserProvidedUnit,
                context = StockMovementContexts.purchase(event.purchaseReferenceNumber),
            )
        }
        stockIncreaseRecorder.record(layerSpecs)
    }
}
