package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.PendingConsolidation
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionKind
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.StockHeldBySales
import me.ezra_home.retail_software_solution.locations.business.stock.api.availability.StockAvailability
import me.ezra_home.retail_software_solution.locations.business.stock.api.availability.StockAvailabilityFetcher
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.ProductReservations
import me.ezra_home.retail_software_solution.locations.business.stock_reservation.api.StockReserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class SaleSessionStockOverlayTest {

    private val sugarId = UUID.randomUUID()
    private val stockAvailabilityFetcher = mock(StockAvailabilityFetcher::class.java)
    private val stockReserver = mock(StockReserver::class.java)
    private val saleSessionStockOverlay = SaleSessionStockOverlay(stockAvailabilityFetcher, stockReserver)

    init {
        // 100 on hand and 30 reserved by drafts leave 70 available.
        `when`(stockAvailabilityFetcher.fetch(listOf(sugarId)))
            .thenReturn(mapOf(sugarId to StockAvailability(BigDecimal("100"), BigDecimal("30"))))
    }

    @Test
    fun `a pending consolidation shows the stock as if its sources were already voided`() {
        val pendingSession = session(
            pendingConsolidation = PendingConsolidation(
                sourceSales = emptyList(),
                sourceSaleReferenceByLineKey = emptyMap(),
                stockHeldBySourceSalesByLocationProductId = mapOf(
                    sugarId to StockHeldBySales(BigDecimal("10"), BigDecimal("6"))
                ),
            )
        )

        val saleLine = saleSessionStockOverlay.populate(pendingSession).saleLines.single()

        verifyNoInteractions(stockReserver)
        assertEquals(0, BigDecimal("106").compareTo(saleLine.storageQuantityOnHand))
        assertEquals(0, BigDecimal("20").compareTo(saleLine.storageQuantityReserved))
        assertEquals(0, BigDecimal("86").compareTo(saleLine.storageQuantityAvailable))
    }

    @Test
    fun `a pending consolidation with nothing given back for a product shows the shop figures`() {
        val pendingSession = session(PendingConsolidation(emptyList(), emptyMap()))

        val saleLine = saleSessionStockOverlay.populate(pendingSession).saleLines.single()

        assertEquals(0, BigDecimal("100").compareTo(saleLine.storageQuantityOnHand))
        assertEquals(0, BigDecimal("30").compareTo(saleLine.storageQuantityReserved))
        assertEquals(0, BigDecimal("70").compareTo(saleLine.storageQuantityAvailable))
    }

    @Test
    fun `a new sale without a saved draft looks up no reservations`() {
        val newSaleSession = session(pendingConsolidation = null)

        saleSessionStockOverlay.populate(newSaleSession)

        verifyNoInteractions(stockReserver)
    }

    @Test
    fun `a saved draft still gets its own reservation back and its on-hand is unchanged`() {
        val saleId = UUID.randomUUID()
        `when`(stockReserver.loadReservationBreakdown(listOf(sugarId)))
            .thenReturn(mapOf(sugarId to ProductReservations(BigDecimal("30"), mapOf(saleId to BigDecimal("8")))))
        val savedDraftSession = session(pendingConsolidation = null, saleId = saleId)

        val saleLine = saleSessionStockOverlay.populate(savedDraftSession).saleLines.single()

        assertEquals(0, BigDecimal("100").compareTo(saleLine.storageQuantityOnHand))
        assertEquals(0, BigDecimal("22").compareTo(saleLine.storageQuantityReserved))
        assertEquals(0, BigDecimal("78").compareTo(saleLine.storageQuantityAvailable))
    }

    private fun session(pendingConsolidation: PendingConsolidation?, saleId: UUID? = null) = SaleSession(
        sessionId = UUID.randomUUID(),
        locationId = UUID.randomUUID(),
        saleId = saleId,
        saleVersion = null,
        productsVersion = 0L,
        productsReservedAtVersion = 0L,
        originalStatus = SaleStatus.DRAFT,
        createdById = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        lastUpdatedAt = OffsetDateTime.now(),
        lastAccessedById = UUID.randomUUID(),
        lastAccessedAt = OffsetDateTime.now(),
        header = SaleSessionHeader(
            referenceNumber = null, contactId = UUID.randomUUID(), soldById = null, dateSold = null, notes = null
        ),
        saleLines = listOf(
            SaleSessionLine(
                identity = SessionIdentity.mintFreshIdentity(),
                locationProductId = sugarId,
                productLabel = "Sugar",
                productPrintLabel = "Sugar",
                userProvidedQuantity = BigDecimal("5"),
                unitId = UUID.randomUUID(),
                baseUnitId = UUID.randomUUID(),
                storageUnitsPerUserProvidedUnit = 1L,
                lineTotal = BigDecimal("75.00"),
                defaultSalePrice = BigDecimal("15.00"),
            )
        ),
        saleAdjustments = emptyList(),
        salePayments = emptyList(),
        totals = SaleSessionTotals.ZERO,
        kind = if (pendingConsolidation == null) SaleSessionKind.SALE else SaleSessionKind.PENDING_CONSOLIDATION,
        pendingConsolidation = pendingConsolidation,
    )
}
