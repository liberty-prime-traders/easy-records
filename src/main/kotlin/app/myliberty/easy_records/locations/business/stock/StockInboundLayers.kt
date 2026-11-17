package app.myliberty.easy_records.locations.business.stock

import app.myliberty.easy_records.locations.business.stock.api.StockAdjustmentMovementDetails
import app.myliberty.easy_records.organizations.business.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class InboundLayerSpec(
    val locationProductId: UUID,
    val sourceType: StockItemSource,
    val movementType: MovementType,
    val documentReference: String,
    val detailReference: String?,
    val storageQuantity: ExactQuantity,
    val batchCost: BigDecimal,
    val userProvidedQuantity: BigDecimal,
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val adjustmentDetails: StockAdjustmentMovementDetails? = null,
    val context: String? = null,
    val priority: Int = 0,
    val activatedOn: OffsetDateTime? = null,
)

object StockInboundLayers {

    fun newLayer(inboundLayerSpec: InboundLayerSpec): StockEntryEntity {
        val storageQuantity = inboundLayerSpec.storageQuantity.toStorageQuantity()
        val unitCost = if (inboundLayerSpec.storageQuantity.isPositive()) {
            Decimals.Scale10.divide(inboundLayerSpec.batchCost, storageQuantity)
        } else {
            BigDecimal.ZERO
        }
        return StockEntryEntity(
            locationProductId = inboundLayerSpec.locationProductId,
            sourceType = inboundLayerSpec.sourceType,
            documentReference = inboundLayerSpec.documentReference,
            detailReference = inboundLayerSpec.detailReference,
            storageBatchSize = storageQuantity,
            storageQuantityRemaining = storageQuantity,
            priority = inboundLayerSpec.priority,
            unitCost = unitCost,
            batchCost = inboundLayerSpec.batchCost,
            costRemaining = inboundLayerSpec.batchCost,
            activatedOn = inboundLayerSpec.activatedOn,
        )
    }

    fun inboundMovement(
        inboundLayerSpec: InboundLayerSpec,
        stockEntry: StockEntryEntity,
        balanceAfterMovement: ExactQuantity,
        quantityBreakdown: QuantityBreakdown
    ): StockMovementEntity = StockMovementCreator.create(
        stockEntryId = stockEntry.id!!,
        locationProductId = inboundLayerSpec.locationProductId,
        movementType = inboundLayerSpec.movementType,
        storageMoved = inboundLayerSpec.storageQuantity,
        balanceAfterMovement = balanceAfterMovement,
        movedCost = inboundLayerSpec.batchCost,
        wholeLineQuantityText = quantityBreakdown.renderUserProvided(inboundLayerSpec.userProvidedQuantity, inboundLayerSpec.unitId),
        documentReference = inboundLayerSpec.documentReference,
        detailReference = inboundLayerSpec.detailReference,
        unitId = inboundLayerSpec.unitId,
        storageUnitsPerUserProvidedUnit = inboundLayerSpec.storageUnitsPerUserProvidedUnit,
        quantityBreakdown = quantityBreakdown,
        adjustmentDetails = inboundLayerSpec.adjustmentDetails,
        context = inboundLayerSpec.context,
    )
}
