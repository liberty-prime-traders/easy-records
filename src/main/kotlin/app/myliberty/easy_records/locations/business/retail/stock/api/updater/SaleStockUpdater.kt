package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
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
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
@TransactionalOnLocationSchema
class SaleStockUpdater(
    private val stockEntryRepository: StockEntryRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val stockBalanceFetcher: StockBalanceFetcher,
    private val productStockBreakdownProvider: ProductStockBreakdownProvider,
    private val entityAdvisoryLock: EntityAdvisoryLock,
    private val netStockDrawReader: NetStockDrawReader,
) {

    fun consumeStock(saleLineStockRequests: List<SaleLineStockRequest>, saleRefNumber: String): List<SaleLineConsumedCost> {
        if (saleLineStockRequests.isEmpty()) return emptyList()
        val locationProductIds = saleLineStockRequests.map { it.locationProductId }
        entityAdvisoryLock.acquire(LockNamespaces.PRODUCT, locationProductIds.toSet())
        val fifoEntriesByLocationProductId = FifoStockConsumer.groupInFifoOrder(
            stockEntryRepository.findFifoEntriesForProducts(locationProductIds)
        )
        val runningBalancesByLocationProductId = stockBalanceFetcher.getLatestBalances(locationProductIds).toMutableMap()
        val productSummariesByLocationProductId = locationProductDataFetcher
            .findSummaryByIds(saleLineStockRequests.map { it.locationProductId }.toSet())
        val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(locationProductIds.toSet())

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val movements = mutableListOf<StockMovementEntity>()
        val saleLineConsumedCosts = saleLineStockRequests.map { saleLineStockRequest ->
            val locationProductId = saleLineStockRequest.locationProductId
            val fifoConsumptionResult = FifoStockConsumer.consume(
                fifoConsumptionRequest = FifoConsumptionRequest(
                    locationProductId = locationProductId,
                    storageQuantity = ExactQuantity.ofStorageFromUserProvided(
                        saleLineStockRequest.userProvidedQuantity,
                        saleLineStockRequest.storageUnitsPerUserProvidedUnit
                    ),
                    userProvidedQuantity = saleLineStockRequest.userProvidedQuantity,
                    unitId = saleLineStockRequest.unitId,
                    storageUnitsPerUserProvidedUnit = saleLineStockRequest.storageUnitsPerUserProvidedUnit,
                    movementType = MovementType.SALE,
                    documentReference = saleRefNumber,
                    detailReference = saleLineStockRequest.saleLineReferenceNumber,
                ),
                fifoEntries = fifoEntriesByLocationProductId[locationProductId].orEmpty(),
                startingBalance = runningBalancesByLocationProductId.getValue(locationProductId),
                quantityBreakdown = breakdownByLocationProductId.getValue(locationProductId),
                productLabel = productSummariesByLocationProductId[locationProductId]?.label ?: locationProductId.toString(),
            )
            runningBalancesByLocationProductId[locationProductId] = fifoConsumptionResult.endingBalance
            modifiedEntries.addAll(fifoConsumptionResult.touchedEntries)
            movements.addAll(fifoConsumptionResult.movements)
            SaleLineConsumedCost(saleLineStockRequest.saleLineId, fifoConsumptionResult.cost)
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(movements)
        return saleLineConsumedCosts
    }

    fun restoreStock(saleRefNumber: String) {
        val netStockDraws = netStockDrawReader.lockAndReadNetDrawsOfDocument(saleRefNumber, MovementType.SALE)
        if (netStockDraws.isEmpty()) return
        val netStockDrawsByLocationProductId = netStockDraws.groupBy { it.locationProductId }
        val stockEntriesById = stockEntryRepository.findAllById(netStockDraws.map { it.stockEntryId }.distinct())
            .associateBy { it.id!! }
        val runningBalancesByLocationProductId = stockBalanceFetcher.getLatestBalances(
            netStockDrawsByLocationProductId.keys.toList()
        ).toMutableMap()
        val breakdownByLocationProductId = productStockBreakdownProvider.forLocationProducts(
            netStockDrawsByLocationProductId.keys
        )

        val modifiedEntries = mutableListOf<StockEntryEntity>()
        val newMovements = mutableListOf<StockMovementEntity>()
        netStockDrawsByLocationProductId.forEach { (locationProductId, netStockDrawsForProduct) ->
            val fifoRestorationResult = FifoStockConsumer.restore(
                netStockDraws = netStockDrawsForProduct,
                stockEntriesById = stockEntriesById,
                startingBalance = runningBalancesByLocationProductId.getValue(locationProductId),
                quantityBreakdown = breakdownByLocationProductId.getValue(locationProductId),
                reversalType = MovementType.SALE_VOID,
            )
            runningBalancesByLocationProductId[locationProductId] = fifoRestorationResult.endingBalance
            modifiedEntries.addAll(fifoRestorationResult.touchedEntries)
            newMovements.addAll(fifoRestorationResult.movements)
        }
        stockEntryRepository.saveAll(modifiedEntries)
        stockMovementRepository.saveAll(newMovements)
    }
}

data class SaleLineConsumedCost(
    val saleLineId: UUID,
    val cost: BigDecimal
)
