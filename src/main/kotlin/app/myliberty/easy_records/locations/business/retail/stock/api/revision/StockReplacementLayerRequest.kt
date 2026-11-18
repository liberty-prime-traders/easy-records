package app.myliberty.easy_records.locations.business.retail.stock.api.revision

import app.myliberty.easy_records.organizations.business.retail.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import java.math.BigDecimal
import java.util.UUID

data class StockReplacementLayerRequest(
    val locationProductId: UUID,
    val sourceType: StockItemSource,
    val movementType: MovementType,
    val documentReference: String,
    val detailReference: String,
    val stockRevisionReferenceNumber: String,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val batchCost: BigDecimal,
    val revisedStockEntryId: UUID,
)
