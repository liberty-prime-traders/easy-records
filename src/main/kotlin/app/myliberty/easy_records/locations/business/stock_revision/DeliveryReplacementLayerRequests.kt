package app.myliberty.easy_records.locations.business.stock_revision

import app.myliberty.easy_records.locations.business.stock.api.revision.StockReplacementLayerRequest
import app.myliberty.easy_records.organizations.business.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

object DeliveryReplacementLayerRequests {

    fun of(
        locationProductId: UUID,
        deliveryReferenceNumber: String,
        replacementLineReferenceNumber: String,
        stockRevisionReferenceNumber: String,
        userProvidedQuantityDelivered: BigDecimal,
        unitId: UUID,
        storageUnitsPerUserProvidedUnit: Long,
        unitCost: BigDecimal,
        revisedStockEntryId: UUID,
    ): StockReplacementLayerRequest = StockReplacementLayerRequest(
        locationProductId = locationProductId,
        sourceType = StockItemSource.PURCHASE,
        movementType = MovementType.PURCHASE_RECEIVED,
        documentReference = deliveryReferenceNumber,
        detailReference = replacementLineReferenceNumber,
        stockRevisionReferenceNumber = stockRevisionReferenceNumber,
        userProvidedQuantity = userProvidedQuantityDelivered,
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
        batchCost = Decimals.MoneyScale.multiply(unitCost, userProvidedQuantityDelivered),
        revisedStockEntryId = revisedStockEntryId,
    )
}
