package app.myliberty.easy_records.locations.business.stock

import app.myliberty.easy_records.locations.business.stock.api.StockAdjustmentMovementDetails
import app.myliberty.easy_records.locations.business.stock.api.revision.StockMovementDraft
import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import java.math.BigDecimal
import java.util.UUID

object StockMovementCreator {

    fun create(
        stockMovementDraft: StockMovementDraft,
        stockEntryId: UUID,
        locationProductId: UUID,
        quantityBreakdown: QuantityBreakdown,
    ): StockMovementEntity = create(
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = stockMovementDraft.movementType,
        storageMoved = stockMovementDraft.storageMoved,
        balanceAfterMovement = stockMovementDraft.balanceAfterMovement,
        movedCost = stockMovementDraft.movedCost,
        wholeLineQuantityText = stockMovementDraft.wholeLineQuantityText,
        documentReference = stockMovementDraft.documentReference,
        detailReference = stockMovementDraft.detailReference,
        unitId = stockMovementDraft.unitId,
        storageUnitsPerUserProvidedUnit = stockMovementDraft.storageUnitsPerUserProvidedUnit,
        quantityBreakdown = quantityBreakdown,
        context = stockMovementDraft.context,
        stockRevisionReference = stockMovementDraft.stockRevisionReference,
    )

    fun create(
        stockEntryId: UUID,
        locationProductId: UUID,
        movementType: MovementType,
        storageMoved: ExactQuantity,
        balanceAfterMovement: ExactQuantity,
        movedCost: BigDecimal,
        wholeLineQuantityText: String,
        documentReference: String?,
        detailReference: String?,
        unitId: UUID,
        storageUnitsPerUserProvidedUnit: Long,
        quantityBreakdown: QuantityBreakdown,
        adjustmentDetails: StockAdjustmentMovementDetails? = null,
        context: String? = null,
        stockRevisionReference: String? = null,
    ): StockMovementEntity = StockMovementEntity(
        stockEntryId = stockEntryId,
        locationProductId = locationProductId,
        movementType = movementType,
        reasonId = adjustmentDetails?.reasonId,
        storageMovedQuantity = storageMoved.toStorageQuantity(),
        wholeLineQuantityText = wholeLineQuantityText,
        storageRemainingQuantity = balanceAfterMovement.toStorageQuantity(),
        movedCost = movedCost,
        storageMovedBreakdownText = quantityBreakdown.render(storageMoved),
        storageRemainingBreakdownText = quantityBreakdown.render(balanceAfterMovement),
        documentReference = documentReference,
        detailReference = detailReference,
        unitId = unitId,
        storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
        category = adjustmentDetails?.category,
        note = adjustmentDetails?.note,
        context = context,
        stockRevisionReference = stockRevisionReference,
    )
}
