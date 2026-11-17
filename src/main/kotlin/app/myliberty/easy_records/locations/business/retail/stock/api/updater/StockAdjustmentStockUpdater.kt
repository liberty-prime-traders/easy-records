package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.stock.FifoStockConsumer
import app.myliberty.easy_records.locations.business.retail.stock.InboundLayerSpec
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryRepository
import app.myliberty.easy_records.locations.business.retail.stock.StockIncreaseRecorder
import app.myliberty.easy_records.locations.business.retail.stock.StockMovementRepository
import app.myliberty.easy_records.locations.business.retail.stock.api.availability.StockAvailabilityValidator
import app.myliberty.easy_records.locations.business.retail.stock.api.fifo.FifoConsumptionRequest
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.organizations.business.retail.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnLocationSchema
class StockAdjustmentStockUpdater(
    private val stockIncreaseRecorder: StockIncreaseRecorder,
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val stockAvailabilityValidator: StockAvailabilityValidator,
    private val entityAdvisoryLock: EntityAdvisoryLock,
) {

    fun increaseStock(stockAdjustmentIncreaseStockRequest: StockAdjustmentIncreaseStockRequest) {
        val inboundLayerSpec = InboundLayerSpec(
            locationProductId = stockAdjustmentIncreaseStockRequest.locationProductId,
            sourceType = StockItemSource.STOCK_ADJUSTMENT,
            movementType = MovementType.STOCK_ADJUSTMENT_INCREASE,
            documentReference = stockAdjustmentIncreaseStockRequest.documentReference,
            detailReference = null,
            storageQuantity = stockAdjustmentIncreaseStockRequest.storageQuantity,
            batchCost = stockAdjustmentIncreaseStockRequest.totalCost,
            userProvidedQuantity = stockAdjustmentIncreaseStockRequest.userProvidedQuantity,
            unitId = stockAdjustmentIncreaseStockRequest.unitId,
            storageUnitsPerUserProvidedUnit = stockAdjustmentIncreaseStockRequest.storageUnitsPerUserProvidedUnit,
            adjustmentDetails = stockAdjustmentIncreaseStockRequest.adjustmentDetails,
        )
        stockIncreaseRecorder.record(listOf(inboundLayerSpec))
    }

    fun decreaseStock(stockAdjustmentDecreaseStockRequest: StockAdjustmentDecreaseStockRequest): StockAdjustmentConsumedCost {
        val locationProductId = stockAdjustmentDecreaseStockRequest.locationProductId
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, setOf(locationProductId))
        val productLabel = stockAdjustmentDecreaseStockRequest.productLabel
        stockAvailabilityValidator.guardSufficientStock(
            mapOf(locationProductId to stockAdjustmentDecreaseStockRequest.storageQuantity),
            mapOf(locationProductId to productLabel),
        )
        val fifoConsumptionResult = FifoStockConsumer.consume(
            fifoConsumptionRequest = FifoConsumptionRequest(
                locationProductId = locationProductId,
                storageQuantity = stockAdjustmentDecreaseStockRequest.storageQuantity,
                userProvidedQuantity = stockAdjustmentDecreaseStockRequest.userProvidedQuantity,
                unitId = stockAdjustmentDecreaseStockRequest.unitId,
                storageUnitsPerUserProvidedUnit = stockAdjustmentDecreaseStockRequest.storageUnitsPerUserProvidedUnit,
                movementType = MovementType.STOCK_ADJUSTMENT_DECREASE,
                documentReference = stockAdjustmentDecreaseStockRequest.documentReference,
                detailReference = null,
                adjustmentDetails = stockAdjustmentDecreaseStockRequest.adjustmentDetails,
            ),
            fifoEntries = FifoStockConsumer.groupInFifoOrder(
                stockEntryRepository.findFifoEntriesForProducts(listOf(locationProductId))
            )[locationProductId].orEmpty(),
            startingBalance = stockBalanceFetcher.getLatestBalances(listOf(locationProductId)).getValue(locationProductId),
            quantityBreakdown = productStockBreakdownProvider.forLocationProduct(locationProductId),
            productLabel = productLabel,
        )
        stockEntryRepository.saveAll(fifoConsumptionResult.touchedEntries)
        stockMovementRepository.saveAll(fifoConsumptionResult.movements)
        return StockAdjustmentConsumedCost(fifoConsumptionResult.cost)
    }
}

@JvmInline
value class StockAdjustmentConsumedCost(val totalCost: BigDecimal)
