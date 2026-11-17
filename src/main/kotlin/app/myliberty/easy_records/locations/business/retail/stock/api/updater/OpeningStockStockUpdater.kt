package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock.InboundLayerSpec
import app.myliberty.easy_records.locations.business.retail.stock.StockIncreaseRecorder
import app.myliberty.easy_records.organizations.business.retail.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class OpeningStockStockUpdater(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
) {

    fun recordOpeningStock(lines: List<OpeningStockLineStockRequest>) {
        val layerSpecs = lines.map { line ->
            InboundLayerSpec(
                locationProductId = line.locationProductId,
                sourceType = StockItemSource.OPENING_STOCK,
                movementType = MovementType.OPENING_STOCK,
                documentReference = line.documentReference,
                detailReference = null,
                storageQuantity = ExactQuantity.ofStorageFromUserProvided(line.userProvidedQuantity, line.storageUnitsPerUserProvidedUnit),
                batchCost = line.batchCost,
                userProvidedQuantity = line.userProvidedQuantity.toBigDecimal(),
                unitId = line.unitId,
                storageUnitsPerUserProvidedUnit = line.storageUnitsPerUserProvidedUnit,
            )
        }
        stockIncreaseRecorder.record(layerSpecs)
    }
}
