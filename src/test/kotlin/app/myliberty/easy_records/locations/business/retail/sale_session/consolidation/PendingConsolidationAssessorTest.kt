package app.myliberty.easy_records.locations.business.sale_session.consolidation

import app.myliberty.easy_records.locations.business.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.sale_session.api.ConsolidationSourceSale
import app.myliberty.easy_records.locations.business.sale_session.api.PendingConsolidation
import app.myliberty.easy_records.locations.business.sale_session.api.RemovedSaleLine
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionKind
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionTotals
import app.myliberty.easy_records.locations.business.sale_session.api.SessionIdentity
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraph
import app.myliberty.easy_records.organizations.business.unitvalue.api.UnitPlacement
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PendingConsolidationAssessorTest {

    private val sugarId = UUID.randomUUID()
    private val pieceUnitId = UUID.randomUUID()
    private val unitConversionGraph = UnitConversionGraph(
        fromUnitToTargets = emptyMap(),
        placementByUnitId = mapOf(pieceUnitId to UnitPlacement(pieceUnitId, 1)),
        breakdownUnits = listOf(BreakdownUnit(pieceUnitId, "piece", 1L, pieceUnitId, OffsetDateTime.now())),
    )

    private fun assess(saleSession: SaleSession, inactiveLocationProductIds: Set<UUID> = emptySet()) =
        PendingConsolidationAssessor.assess(saleSession, inactiveLocationProductIds, unitConversionGraph)

    @Test
    fun `lines that merge cleanly leave no issue and the sale can be generated`() {
        val firstLine = saleLine("2", "30.00", "15.00")
        val secondLine = saleLine("3", "45.00", "15.00")

        val assessment = assess(
            pendingSession(listOf(firstLine, secondLine), mapOf(firstLine.lineKey to "S1", secondLine.lineKey to "S2"))
        )

        assertTrue(assessment.consolidationIssues.isEmpty())
        assertTrue(assessment.canGenerateConsolidatedSale)
    }

    @Test
    fun `different list prices give one issue naming both sales and block generation`() {
        val firstLine = saleLine("2", "30.00", "15.00")
        val secondLine = saleLine("3", "60.00", "20.00")

        val assessment = assess(
            pendingSession(listOf(firstLine, secondLine), mapOf(firstLine.lineKey to "S1", secondLine.lineKey to "S2"))
        )

        val consolidationIssue = assessment.consolidationIssues.single()
        assertEquals("S1, S2", consolidationIssue.saleReferences)
        assertTrue(consolidationIssue.description.contains("different list prices"))
        val expectedLinesText = "2 pieces at KES 15.00 per piece from S1; " +
            "3 pieces at KES 20.00 per piece from S2"
        assertTrue(consolidationIssue.fix.contains(expectedLinesText))
        assertFalse(assessment.canGenerateConsolidatedSale)
    }

    @Test
    fun `removing one of the conflicting lines clears the issue`() {
        val keptLine = saleLine("2", "30.00", "15.00")

        val assessment = assess(
            pendingSession(listOf(keptLine), mapOf(keptLine.lineKey to "S1"))
        )

        assertTrue(assessment.canGenerateConsolidatedSale)
    }

    @Test
    fun `a removed line is not assessed, even when it would conflict with a held line`() {
        val keptLine = saleLine("2", "30.00", "15.00")
        val removedLine = saleLine("3", "60.00", "20.00")
        val session = pendingSession(listOf(keptLine), mapOf(keptLine.lineKey to "S1", removedLine.lineKey to "S2"))
        val sessionWithRemovedLine = session.copy(
            pendingConsolidation = session.pendingConsolidation!!.copy(
                removedSaleLines = listOf(RemovedSaleLine(removedLine, emptyList()))
            )
        )

        val assessment = assess(sessionWithRemovedLine)

        assertTrue(assessment.canGenerateConsolidatedSale)
    }

    @Test
    fun `an inactive product is an issue even when its lines would merge`() {
        val firstLine = saleLine("2", "30.00", "15.00")
        val secondLine = saleLine("3", "45.00", "15.00")
        val assessment = assess(
            pendingSession(listOf(firstLine, secondLine), mapOf(firstLine.lineKey to "S1", secondLine.lineKey to "S2")),
            inactiveLocationProductIds = setOf(sugarId),
        )

        assertTrue(assessment.consolidationIssues.single().description.contains("no longer active"))
        assertFalse(assessment.canGenerateConsolidatedSale)
    }

    @Test
    fun `sources with a sale-wide adjustment give a warning that does not block generation`() {
        val heldLine = saleLine("1", "15.00", "15.00")
        val heldSession = pendingSession(listOf(heldLine), mapOf(heldLine.lineKey to "S1"))
        val sessionWithDiscountedSource = heldSession.copy(
            pendingConsolidation = heldSession.requiredPendingConsolidation().copy(
                saleReferencesWithOrderLevelAdjustments = setOf("S1"),
            )
        )

        val assessment = assess(sessionWithDiscountedSource)

        val warning = assessment.consolidationIssues.single()
        assertEquals("S1", warning.saleReferences)
        assertFalse(warning.blocking)
        assertTrue(assessment.canGenerateConsolidatedSale)
    }

    @Test
    fun `an ordinary sale session is refused`() {
        val ordinarySession = pendingSession(emptyList(), emptyMap()).copy(
            kind = SaleSessionKind.SALE,
            pendingConsolidation = null,
        )

        assertThrows(RtsGenericException::class.java) { assess(ordinarySession) }
    }

    private fun saleLine(quantity: String, lineTotal: String, defaultSalePrice: String) = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = sugarId,
        productLabel = "Sugar",
        productPrintLabel = "Sugar",
        userProvidedQuantity = BigDecimal(quantity),
        unitId = pieceUnitId,
        baseUnitId = pieceUnitId,
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal(lineTotal),
        defaultSalePrice = BigDecimal(defaultSalePrice),
    )

    private fun pendingSession(
        saleLines: List<SaleSessionLine>,
        sourceSaleReferenceByLineKey: Map<UUID, String>,
    ) = SaleSession(
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
        saleLines = saleLines,
        saleAdjustments = emptyList(),
        salePayments = emptyList(),
        totals = SaleSessionTotals.ZERO,
        kind = SaleSessionKind.PENDING_CONSOLIDATION,
        pendingConsolidation = PendingConsolidation(
            sourceSales = listOf(ConsolidationSourceSale(UUID.randomUUID(), 0L, "S1")),
            sourceSaleReferenceByLineKey = sourceSaleReferenceByLineKey,
        ),
    )
}
