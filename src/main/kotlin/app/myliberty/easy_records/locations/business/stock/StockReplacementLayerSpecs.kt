package app.myliberty.easy_records.locations.business.stock

import app.myliberty.easy_records.locations.business.stock.api.StockMovementContexts
import app.myliberty.easy_records.locations.business.stock.api.revision.StockReplacementLayerRequest
import app.myliberty.easy_records.util.quantity.ExactQuantity

object StockReplacementLayerSpecs {

    fun of(
        stockReplacementLayerRequest: StockReplacementLayerRequest,
        revisedStockEntry: StockEntryEntity
    ): InboundLayerSpec = InboundLayerSpec(
        locationProductId = stockReplacementLayerRequest.locationProductId,
        sourceType = stockReplacementLayerRequest.sourceType,
        movementType = stockReplacementLayerRequest.movementType,
        documentReference = stockReplacementLayerRequest.documentReference,
        detailReference = stockReplacementLayerRequest.detailReference,
        storageQuantity = ExactQuantity.ofStorageFromUserProvided(
            stockReplacementLayerRequest.userProvidedQuantity,
            stockReplacementLayerRequest.storageUnitsPerUserProvidedUnit
        ),
        batchCost = stockReplacementLayerRequest.batchCost,
        userProvidedQuantity = stockReplacementLayerRequest.userProvidedQuantity,
        unitId = stockReplacementLayerRequest.unitId,
        storageUnitsPerUserProvidedUnit = stockReplacementLayerRequest.storageUnitsPerUserProvidedUnit,
        context = StockMovementContexts.replacementFromStockRevision(
            stockReplacementLayerRequest.stockRevisionReferenceNumber
        ),
        activatedOn = revisedStockEntry.requiredActivatedOn(),
    )
}
