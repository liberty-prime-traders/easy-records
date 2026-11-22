package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionBuilder
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionCopier
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionStockOverlay
import app.myliberty.easy_records.locations.business.retail.sale_session.api.ConsolidationSourceSale
import app.myliberty.easy_records.locations.business.retail.sale_session.api.PendingConsolidation
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionKind
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.support.anyNonNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class PendingConsolidationPromoterTest {

    private val sugarProductId = UUID.randomUUID()
    private val riceProductId = UUID.randomUUID()
    private val sourceSaleId = UUID.randomUUID()
    private val locationProductDataFetcher = mock(LocationProductDataFetcher::class.java)
    private val saleDataFetcher = mock(SaleDataFetcher::class.java)
    private val saleSessionCopier = mock(SaleSessionCopier::class.java)
    private val saleSessionStockOverlay = mock(SaleSessionStockOverlay::class.java)
    private val pendingConsolidationPromoter = PendingConsolidationPromoter(
        locationProductDataFetcher, saleDataFetcher, saleSessionCopier, saleSessionStockOverlay
    )

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = UUID.randomUUID(),
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1"),
            )
        )
    }

    private fun saleLine(locationProductId: UUID, label: String, rank: String) = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = locationProductId,
        productLabel = label,
        productPrintLabel = label,
        userProvidedQuantity = BigDecimal.ONE,
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal("10.00"),
        defaultSalePrice = BigDecimal("10.00"),
        rank = BigDecimal(rank),
    )

    private fun pendingSession(heldLine: SaleSessionLine, cleanLocationProductIds: Set<UUID>): SaleSession =
        SaleSessionBuilder(UUID.randomUUID())
            .withHeader(SaleSessionHeader(null, UUID.randomUUID(), null, null, null))
            .withSaleLines(listOf(heldLine))
            .asPendingConsolidation(
                PendingConsolidation(
                    sourceSales = listOf(ConsolidationSourceSale(sourceSaleId, 0L, "S1")),
                    sourceSaleReferenceByLineKey = mapOf(heldLine.lineKey to "S1"),
                    cleanLocationProductIds = cleanLocationProductIds,
                )
            )
            .build()

    @Test
    fun `a clean product that is still active stays off the form`() {
        val session = pendingSession(saleLine(sugarProductId, "Sugar", "1"), setOf(riceProductId))
        `when`(locationProductDataFetcher.findInactiveIds(setOf(riceProductId))).thenReturn(emptySet())

        val promoted = pendingConsolidationPromoter.promote(session)

        assertSame(session, promoted)
        verifyNoInteractions(saleDataFetcher, saleSessionCopier)
    }

    @Test
    fun `a clean product that turns inactive is brought onto the form with its sale reference`() {
        val heldSugarLine = saleLine(sugarProductId, "Sugar", "1")
        val riceLineInSource = saleLine(riceProductId, "Rice", "2")
        val session = pendingSession(heldSugarLine, setOf(riceProductId))
        `when`(locationProductDataFetcher.findInactiveIds(setOf(riceProductId))).thenReturn(setOf(riceProductId))
        val sourceSaleHeaders = emptyList<SaleHeaderDto>()
        `when`(saleDataFetcher.getSaleHeaders(listOf(sourceSaleId))).thenReturn(sourceSaleHeaders)
        val consolidationCopy = ConsolidationCopy(
            saleSession = session.copy(saleLines = listOf(heldSugarLine, riceLineInSource)),
            sourceSaleReferenceByLineKey = mapOf(heldSugarLine.lineKey to "S1", riceLineInSource.lineKey to "S1"),
            sourceSaleLinesBySaleId = emptyMap(),
            orderLevelAmountByAdjustmentKey = emptyMap(),
            saleReferencesWithOrderLevelAdjustments = emptySet(),
        )
        `when`(saleSessionCopier.copyFromSalesWithSourceReferences(session.sessionId, sourceSaleHeaders))
            .thenReturn(consolidationCopy)
        `when`(saleSessionStockOverlay.populate(anyNonNull(SaleSession::class.java))).thenAnswer { it.arguments[0] }

        val promoted = pendingConsolidationPromoter.promote(session)

        assertEquals(listOf("Sugar", "Rice"), promoted.saleLines.map { it.productLabel })
        val promotedPending = promoted.requiredPendingConsolidation()
        assertEquals(emptySet<UUID>(), promotedPending.cleanLocationProductIds)
        assertEquals("S1", promotedPending.sourceSaleReferenceByLineKey.getValue(riceLineInSource.lineKey))
    }

    @Test
    fun `an ordinary sale session is returned as it is`() {
        val ordinarySession = pendingSession(saleLine(sugarProductId, "Sugar", "1"), emptySet())
            .copy(kind = SaleSessionKind.SALE, pendingConsolidation = null)

        assertSame(ordinarySession, pendingConsolidationPromoter.promote(ordinarySession))
    }

}
