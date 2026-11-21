package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.stock.InboundLayerSpec
import app.myliberty.easy_records.locations.business.retail.stock.StockIncreaseRecorder
import app.myliberty.easy_records.locations.business.retail.stock.StockMovementRepository
import app.myliberty.easy_records.organizations.business.retail.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraphFacade
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class StockTransferReceiptStockUpdater(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    // Lines that already have a TRANSFER_IN movement are skipped, so a repeated call never credits a line twice.
    // The product lock comes before that check: the check and the credit are one decision.
    fun recordTransferReceipt(
        dispatchReferenceNumber: String,
        stockTransferReceiptLineStockRequests: List<StockTransferReceiptLineStockRequest>
    ) {
        val orgProductIds = stockTransferReceiptLineStockRequests.map { it.orgProductId }.toSet()
        val identitiesByOrgProductId = locationProductDataFetcher.findIdentitiesByOrgProductIds(orgProductIds)
        val locationProductIdByOrgProductId = orgProductIds.associateWith { orgProductId ->
            identitiesByOrgProductId[orgProductId]?.locationProductId
                ?: throw LibertyGenericException("No location product found for product $orgProductId at destination")
        }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIdByOrgProductId.values.toSet())

        val creditedLineReferences = stockMovementRepository.findPresentDetailReferences(
            dispatchReferenceNumber,
            stockTransferReceiptLineStockRequests.map { it.dispatchLineReferenceNumber },
            MovementType.TRANSFER_IN
        )
        val linesToCredit = stockTransferReceiptLineStockRequests
            .filter { it.dispatchLineReferenceNumber !in creditedLineReferences }
        if (linesToCredit.isEmpty()) return

        guardSameStorageUnit(linesToCredit, locationProductIdByOrgProductId)

        val layerSpecs = linesToCredit.map { line ->
            InboundLayerSpec(
                locationProductId = locationProductIdByOrgProductId.getValue(line.orgProductId),
                sourceType = StockItemSource.TRANSFER_IN,
                movementType = MovementType.TRANSFER_IN,
                documentReference = dispatchReferenceNumber,
                detailReference = line.dispatchLineReferenceNumber,
                storageQuantity = ExactQuantity.ofStorage(line.storageQuantityDispatched),
                batchCost = line.costDispatched,
                userProvidedQuantity = line.wholeLineUserProvidedQuantity,
                unitId = line.unitId,
                storageUnitsPerUserProvidedUnit = line.storageUnitsPerUserProvidedUnit,
            )
        }
        stockIncreaseRecorder.record(layerSpecs)
    }

    // The dispatch quantity is in the source's storage unit, so the destination must count stock in the same root
    private fun guardSameStorageUnit(
        stockTransferReceiptLineStockRequests: List<StockTransferReceiptLineStockRequest>,
        locationProductIdByOrgProductId: Map<UUID, UUID>
    ) {
        val unitConversionGraph = unitConversionGraphFacade.getOrLoad()
        val baseUnitIdByLocationProductId = locationProductDataFetcher.getBaseUnitIds(locationProductIdByOrgProductId.values)
        stockTransferReceiptLineStockRequests.forEach { line ->
            val destinationBaseUnitId = baseUnitIdByLocationProductId.getValue(locationProductIdByOrgProductId.getValue(line.orgProductId))
            if (unitConversionGraph.getStorageUnitId(line.unitId) != unitConversionGraph.getStorageUnitId(destinationBaseUnitId)) {
                throw LibertyGenericException(SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER)
            }
        }
    }

    companion object {
        const val SOURCE_AND_DESTINATION_STORAGE_UNITS_DIFFER =
            "The source and destination products count stock in different storage units, so the transfer cannot be received"
    }
}
