package app.myliberty.easy_records.locations.business.retail.stock

import app.myliberty.easy_records.locations.business.retail.location_product.api.ProductStockBreakdownProvider
import app.myliberty.easy_records.locations.business.core.lock.api.EntityAdvisoryLock
import app.myliberty.easy_records.locations.business.retail.stock.api.read.StockBalanceFetcher
import app.myliberty.easy_records.organizations.business.retail.stock_item_source.api.StockItemSource
import app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class StockIncreaseRecorderTest {

    private val pieceId = UUID.randomUUID()
    private val firstLocationProductId = UUID.randomUUID()
    private val secondLocationProductId = UUID.randomUUID()
    private val pieceBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(pieceId, "Piece", 1L, pieceId, OffsetDateTime.parse("2026-01-01T00:00:00Z"))), pieceId
    )

    private val stockEntryRepository = mock(StockEntryRepository::class.java)
    private val stockMovementRepository = mock(StockMovementRepository::class.java)
    private val stockBalanceFetcher = mock(StockBalanceFetcher::class.java)
    private val productStockBreakdownProvider = mock(ProductStockBreakdownProvider::class.java)
    private val entityAdvisoryLock = mock(EntityAdvisoryLock::class.java)
    private val stockIncreaseRecorder = StockIncreaseRecorder(
        stockEntryRepository,
        stockMovementRepository,
        stockBalanceFetcher,
        productStockBreakdownProvider,
        entityAdvisoryLock
    )

    private val savedMovements = mutableListOf<StockMovementEntity>()

    init {
        `when`(stockMovementRepository.saveAll(anyList<StockMovementEntity>())).thenAnswer { invocation ->
            invocation.getArgument<List<StockMovementEntity>>(0).also { savedMovements.addAll(it) }
        }
    }

    private fun spec(locationProductId: UUID, quantity: String, detailReference: String) = InboundLayerSpec(
        locationProductId = locationProductId,
        sourceType = StockItemSource.TRANSFER_IN,
        movementType = MovementType.TRANSFER_IN,
        documentReference = "DISPATCH-1",
        detailReference = detailReference,
        storageQuantity = ExactQuantity.ofStorage(BigDecimal(quantity)),
        batchCost = BigDecimal("10.0000"),
        userProvidedQuantity = BigDecimal(quantity),
        unitId = pieceId,
        storageUnitsPerUserProvidedUnit = 1L,
    )

    @Test
    fun `each product's running balance carries across its own lines and never leaks into another product`() {
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(
            mapOf(firstLocationProductId to BigDecimal("100"), secondLocationProductId to BigDecimal("5"))
        )
        `when`(productStockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(
            mapOf(firstLocationProductId to pieceBreakdown, secondLocationProductId to pieceBreakdown)
        )
        `when`(stockEntryRepository.save(any(StockEntryEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<StockEntryEntity>(0).also { it.id = UUID.randomUUID() }
        }

        stockIncreaseRecorder.record(
            listOf(
                spec(firstLocationProductId, "10", "LINE-1"),
                spec(secondLocationProductId, "3", "LINE-2"),
                spec(firstLocationProductId, "20", "LINE-3"),
            )
        )

                assertEquals(
            listOf(BigDecimal("110.0000"), BigDecimal("8.0000"), BigDecimal("130.0000")),
            savedMovements.map { it.storageRemainingQuantity }
        )
        assertEquals(listOf("LINE-1", "LINE-2", "LINE-3"), savedMovements.map { it.detailReference })
    }

    @Test
    fun `returned entry ids follow spec order and each layer keeps its own priority`() {
        `when`(
            stockBalanceFetcher.getLatestBalances(anyCollection())
        ).thenReturn(mapOf(firstLocationProductId to BigDecimal("0")))
        `when`(
            productStockBreakdownProvider.forLocationProducts(anyCollection())
        ).thenReturn(mapOf(firstLocationProductId to pieceBreakdown))
        val savedEntries = mutableListOf<StockEntryEntity>()
        `when`(stockEntryRepository.save(any(StockEntryEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<StockEntryEntity>(0).also { it.id = UUID.randomUUID(); savedEntries.add(it) }
        }

        val entryIds = stockIncreaseRecorder.record(
            listOf(
                spec(firstLocationProductId, "10", "LINE-1").copy(priority = -1000),
                spec(firstLocationProductId, "20", "LINE-2"),
            )
        )

        assertEquals(savedEntries.map { it.id }, entryIds)
        assertEquals(listOf(-1000, 0), savedEntries.map { it.priority })
    }

    @Test
    fun `a single spec returns the id of the one layer it opened`() {
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(mapOf(firstLocationProductId to BigDecimal("0")))
        `when`(productStockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(mapOf(firstLocationProductId to pieceBreakdown))
        var savedEntry: StockEntryEntity? = null
        `when`(stockEntryRepository.save(any(StockEntryEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<StockEntryEntity>(0).also { it.id = UUID.randomUUID(); savedEntry = it }
        }

        val entryId = stockIncreaseRecorder.record(spec(firstLocationProductId, "10", "LINE-1"))

        assertEquals(savedEntry!!.id, entryId)
    }

    @Test
    fun `no specs means no lock, no read and no write`() {
        stockIncreaseRecorder.record(emptyList())

        verifyNoInteractions(entityAdvisoryLock, stockBalanceFetcher, stockEntryRepository, stockMovementRepository)
    }

    private fun stubBalancesBreakdownsAndEntrySaves() {
        `when`(stockBalanceFetcher.getLatestBalances(anyCollection())).thenReturn(
            mapOf(firstLocationProductId to BigDecimal("0"), secondLocationProductId to BigDecimal("0"))
        )
        `when`(productStockBreakdownProvider.forLocationProducts(anyCollection())).thenReturn(
            mapOf(firstLocationProductId to pieceBreakdown, secondLocationProductId to pieceBreakdown)
        )
        `when`(stockEntryRepository.save(any(StockEntryEntity::class.java))).thenAnswer { invocation ->
            invocation.getArgument<StockEntryEntity>(0).also { it.id = UUID.randomUUID() }
        }
    }

    @Test
    fun `the product lock is taken for the increased product before its balance is read or any stock is written`() {
        stubBalancesBreakdownsAndEntrySaves()

        stockIncreaseRecorder.record(spec(firstLocationProductId, "10", "LINE-1"))

        val lockBeforeReadAndWrites = inOrder(
            entityAdvisoryLock, stockBalanceFetcher, stockEntryRepository, stockMovementRepository
        )
        lockBeforeReadAndWrites.verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, setOf(firstLocationProductId))
        lockBeforeReadAndWrites.verify(stockBalanceFetcher).getLatestBalances(listOf(firstLocationProductId))
        lockBeforeReadAndWrites.verify(stockEntryRepository).save(any(StockEntryEntity::class.java))
        lockBeforeReadAndWrites.verify(stockMovementRepository).saveAll(anyList<StockMovementEntity>())
    }

    @Test
    fun `every distinct product in a batch is locked once and a repeated product is not locked twice`() {
        stubBalancesBreakdownsAndEntrySaves()

        stockIncreaseRecorder.record(
            listOf(
                spec(firstLocationProductId, "10", "LINE-1"),
                spec(secondLocationProductId, "3", "LINE-2"),
                spec(firstLocationProductId, "20", "LINE-3"),
            )
        )

        verify(entityAdvisoryLock, times(1)).acquire(
            LockNamespaces.PRODUCT, setOf(firstLocationProductId, secondLocationProductId)
        )
        verify(entityAdvisoryLock, times(1)).acquire(
            anyString(), anyCollection<UUID>()
        )
    }

    @Test
    fun `each separate increase takes its own lock for its own product`() {
        stubBalancesBreakdownsAndEntrySaves()

        stockIncreaseRecorder.record(spec(firstLocationProductId, "10", "LINE-1"))
        stockIncreaseRecorder.record(spec(secondLocationProductId, "3", "LINE-2"))

        verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, setOf(firstLocationProductId))
        verify(entityAdvisoryLock).acquire(LockNamespaces.PRODUCT, setOf(secondLocationProductId))
    }
}
