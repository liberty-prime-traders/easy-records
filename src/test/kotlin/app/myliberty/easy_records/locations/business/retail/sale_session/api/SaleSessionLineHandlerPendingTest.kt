package app.myliberty.easy_records.locations.business.retail.sale_session.api

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionInMemoryStore
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionLineAdjustmentReconciler
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionLineChangeContextBuilder
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionStockOverlay
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionUpdateFinalizer
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionValidator
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.retail.stock_reservation.api.StockReserver
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class SaleSessionLineHandlerPendingTest {

    private var sessionHandedToFinalizer: SaleSession? = null
    private val saleSessionStore = SaleSessionInMemoryStore()
    private val saleSessionLineHandler = SaleSessionLineHandler(
        saleSessionStore = saleSessionStore,
        saleSessionValidator = SaleSessionValidator(
            mock(AdjustmentReasonService::class.java), mock(LocationProductService::class.java)
        ),
        saleSessionUpdateFinalizer = mock(SaleSessionUpdateFinalizer::class.java) { invocation ->
            sessionHandedToFinalizer = invocation.getArgument(0)
            null
        },
        saleSessionLineChangeContextBuilder = mock(SaleSessionLineChangeContextBuilder::class.java),
        saleSessionStockOverlay = mock(SaleSessionStockOverlay::class.java) { invocation -> invocation.getArgument(0) },
        stockReserver = mock(StockReserver::class.java),
        saleSessionLineAdjustmentReconciler = mock(SaleSessionLineAdjustmentReconciler::class.java),
    )

    private val heldLine = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = UUID.randomUUID(),
        productLabel = "Sugar",
        productPrintLabel = "Sugar",
        userProvidedQuantity = BigDecimal("2"),
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal("30.00"),
        defaultSalePrice = BigDecimal("15.00"),
    )

    private val pendingSession = SaleSession(
        sessionId = UUID.randomUUID(),
        locationId = UUID.randomUUID(),
        saleId = null,
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
        saleLines = listOf(heldLine),
        saleAdjustments = emptyList(),
        salePayments = emptyList(),
        totals = SaleSessionTotals.ZERO,
        kind = SaleSessionKind.PENDING_CONSOLIDATION,
        pendingConsolidation = PendingConsolidation(emptyList(), mapOf(heldLine.lineKey to "S1")),
    ).also(saleSessionStore::save)

    @Test
    fun `a pending consolidation refuses an added line before anything is loaded`() {
        val lineRequestDto = SaleSessionLineRequestDto(
            additions = listOf(
                SaleSessionLineAddDto(UUID.randomUUID(), PositiveUserProvidedQuantity.of(BigDecimal.ONE))
            )
        )

        assertThrows(RtsGenericException::class.java) {
            saleSessionLineHandler.applyLineChanges(pendingSession.sessionId, lineRequestDto)
        }
    }

    @Test
    fun `removing a line from a pending consolidation moves it to the removed lines`() {
        saleSessionLineHandler.removeLine(pendingSession.sessionId, SaleSessionRowIdentityDto(heldLine.identity))

        val finalizedSession = sessionHandedToFinalizer!!
        assertEquals(emptyList<SaleSessionLine>(), finalizedSession.saleLines)
        assertEquals(heldLine, finalizedSession.pendingConsolidation!!.removedSaleLines.single().saleLine)
    }

    @Test
    fun `restoring a removed line puts it back`() {
        saleSessionLineHandler.removeLine(pendingSession.sessionId, SaleSessionRowIdentityDto(heldLine.identity))
        saleSessionStore.save(sessionHandedToFinalizer!!)

        saleSessionLineHandler.restoreLine(pendingSession.sessionId, SaleSessionRowIdentityDto(heldLine.identity))

        assertEquals(listOf(heldLine), sessionHandedToFinalizer!!.saleLines)
    }
}
