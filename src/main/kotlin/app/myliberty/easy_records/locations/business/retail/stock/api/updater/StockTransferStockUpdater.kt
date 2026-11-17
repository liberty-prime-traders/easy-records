package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.stock.FifoStockConsumer
import app.myliberty.easy_records.locations.business.retail.stock.NetStockDrawReader
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryEntity
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryRepository
import app.myliberty.easy_records.locations.business.retail.stock.StockMovementEntity
import app.myliberty.easy_records.locations.business.retail.stock.StockMovementRepository
import app.myliberty.easy_records.locations.business.retail.stock.api.fifo.FifoConsumptionRequest
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import org.springframework.stereotype.Service

@Service
@TransactionalOnLocationSchema
class StockTransferStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val netStockDrawReader: NetStockDrawReader,
) {

    fun consumeStockForDispatch(dispatchLineRequests: List<StockTransferDispatchLineStockRequest>) {
        if (dispatchLineRequests.isEmpty()) return
        val locationProductIds = dispatchLineRequests.map { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds.toSet())
        val fifoEntriesByLocationProductId = FifoStockConsumer.groupInFifoOrder(
            stockEntryRepository.findFifoEntriesForProducts(locationProductIds)
        )
        val balancesByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds)
        val productSummariesByLocationProductId = locationProductDataFetcher.findSummaryByIds(locationProductIds.toSet())
        val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(locationProductIds.toSet())

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        dispatchLineRequests.groupBy { it.locationProductId }.forEach { (locationProductId, requestsForProduct) ->
            val productLabel = productSummariesByLocationProductId[locationProductId]?.label
                ?: locationProductId.toString()
            var runningBalance = balancesByLocationProductId.getValue(locationProductId)
            requestsForProduct.forEach { dispatchLineRequest ->
                val fifoConsumptionResult = FifoStockConsumer.consume(
                    fifoConsumptionRequest = FifoConsumptionRequest(
                        locationProductId = locationProductId,
                        storageQuantity = dispatchLineRequest.storageQuantity,
                        userProvidedQuantity = dispatchLineRequest.userProvidedQuantity,
                        unitId = dispatchLineRequest.unitId,
                        storageUnitsPerUserProvidedUnit = dispatchLineRequest.storageUnitsPerUserProvidedUnit,
                        movementType = MovementType.TRANSFER_OUT,
                        documentReference = dispatchLineRequest.dispatchReference,
                        detailReference = dispatchLineRequest.dispatchLineReference,
                    ),
                    fifoEntries = fifoEntriesByLocationProductId[locationProductId].orEmpty(),
                    startingBalance = runningBalance,
                    quantityBreakdown = breakdownByLocationProductId.getValue(locationProductId),
                    productLabel = productLabel,
                )
                runningBalance = fifoConsumptionResult.endingBalance
                modifiedEntries.addAll(fifoConsumptionResult.touchedEntries)
                movements.addAll(fifoConsumptionResult.movements)
            }
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(movements)
    }

    fun restoreStockForLines(dispatchReference: String, dispatchLineReferenceNumbers: Collection<String>) {
        if (dispatchLineReferenceNumbers.isEmpty()) return
        val netStockDraws = netStockDrawReader.lockAndReadNetDrawsOfLines(
            dispatchReference, dispatchLineReferenceNumbers, MovementType.TRANSFER_OUT
        )
        if (netStockDraws.isEmpty()) return

        val stockEntriesById = stockEntryRepository
            .findAllById(netStockDraws.map { it.stockEntryId }.distinct())
            .associateBy { it.id!! }
        val balancesByLocationProductId = stockBalanceFetcher.getLatestBalances(
            netStockDraws.map { it.locationProductId }.distinct()
        )
        val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
            netStockDraws.map { it.locationProductId }.toSet()
        )

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val newMovements = mutableListOf<StockMovementEntity>()
        netStockDraws.groupBy { it.locationProductId }.forEach { (locationProductId, netStockDrawsForProduct) ->
            val fifoRestorationResult = FifoStockConsumer.restore(
                netStockDraws = netStockDrawsForProduct,
                stockEntriesById = stockEntriesById,
                startingBalance = balancesByLocationProductId.getValue(locationProductId),
                quantityBreakdown = breakdownByLocationProductId.getValue(locationProductId),
                reversalType = MovementType.TRANSFER_CANCELLED,
            )
            modifiedEntries.addAll(fifoRestorationResult.touchedEntries)
            newMovements.addAll(fifoRestorationResult.movements)
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(newMovements)
    }
}
