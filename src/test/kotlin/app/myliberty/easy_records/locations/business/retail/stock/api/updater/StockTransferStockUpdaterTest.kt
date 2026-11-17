package app.myliberty.easy_records.locations.business.retail.stock.api.updater

import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryEntity
import app.myliberty.easy_records.locations.business.retail.stock.NetStockDrawReader
import app.myliberty.easy_records.locations.business.retail.stock.StockEntryRepository
import app.myliberty.easy_records.locations.business.retail.stock.StockMovementEntity
import app.myliberty.easy_records.locations.business.retail.stock.StockMovementRepository
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.mockito.ArgumentMatchers.anyCollection
import java.time.OffsetDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

@WithDefaultOrganizationSession
class StockTransferStockUpdaterTest {

    @Test
    fun `consumeStockForDispatch accumulates a running balance across multiple dispatch lines for the same product`() {
        val locationProductId = UUID.randomUUID()
        val pieceId = UUID.randomUUID()
        val firstDispatchLineRequest = StockTransferDispatchLineStockRequest(
            dispatchReference = "DISPATCH-1",
            dispatchLineReference = "DISPATCH-LINE-1",
            locationProductId = locationProductId,
            storageQuantity = ExactQuantity.ofStorage(BigDecimal("10.0000")),
            userProvidedQuantity = BigDecimal("10.0000"),
            unitId = pieceId,
            storageUnitsPerUserProvidedUnit = 1L,
        )
        val secondDispatchLineRequest = StockTransferDispatchLineStockRequest(
            dispatchReference = "DISPATCH-1",
            dispatchLineReference = "DISPATCH-LINE-2",
            locationProductId = locationProductId,
            storageQuantity = ExactQuantity.ofStorage(BigDecimal("15.0000")),
            userProvidedQuantity = BigDecimal("15.0000"),
            unitId = pieceId,
            storageUnitsPerUserProvidedUnit = 1L,
        )
        val locationProductIds = listOf(locationProductId, locationProductId)

        val stockEntry = StockEntryEntity(
            locationProductId = locationProductId,
            storageBatchSize = BigDecimal("100.0000"),
            storageQuantityRemaining = BigDecimal("100.0000"),
            priority = 0,
            unitCost = BigDecimal("5.00"),
            batchCost = BigDecimal("500.0000"),
            costRemaining = BigDecimal("500.0000"),
        )
        stockEntry.id = UUID.randomUUID()

        val stockEntryRepository = mock(StockEntryRepository::class.java)
        val stockMovementRepository = mock(StockMovementRepository::class.java)
        val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
        val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
        val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
        val productStockBreakdownProvider = mock(ProductStockBreakdownProvider::class.java)
        `when`(productStockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(
            mapOf(
                locationProductId to QuantityBreakdown.forBaseUnit(
                    listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, OffsetDateTime.now())), pieceId
                )
            )
        )

        `when`(stockEntryRepository.findFifoEntriesForProducts(locationProductIds)).thenReturn(listOf(stockEntry))
        `when`(stockBalanceFetcher.getLatestBalances(locationProductIds))
            .thenReturn(mapOf(locationProductId to BigDecimal("100.0000")))
        `when`(locationProductDataFetcher.findSummaryByIds(setOf(locationProductId))).thenReturn(emptyMap())

        val stockTransferStockUpdater = StockTransferStockUpdater(
            stockEntryRepository = stockEntryRepository,
            stockMovementRepository = stockMovementRepository,
            locationProductDataFetcher = locationProductDataFetcher,
            stockBalanceFetcher = stockBalanceFetcher,
            productStockBreakdownProvider = productStockBreakdownProvider,
            entityAdvisoryLock = entityAdvisoryLock,
            netStockDrawReader = mock(NetStockDrawReader::class.java),
        )

        stockTransferStockUpdater.consumeStockForDispatch(listOf(firstDispatchLineRequest, secondDispatchLineRequest))

        @Suppress("UNCHECKED_CAST")
        val movementsCaptor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<StockMovementEntity>>
        verify(stockMovementRepository).saveAll(movementsCaptor.capture())
        val movementsByDispatchLineRef = movementsCaptor.value.associateBy { it.detailReference }

        // Second line's balance must reflect the first line's consumption, not the original pre-dispatch balance.
        assertEquals(BigDecimal("90.0000"), movementsByDispatchLineRef.getValue("DISPATCH-LINE-1").storageRemainingQuantity)
        assertEquals(BigDecimal("75.0000"), movementsByDispatchLineRef.getValue("DISPATCH-LINE-2").storageRemainingQuantity)
    }
}
