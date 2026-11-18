package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class ConsolidationProblemStoryWriterTest {

    private val locationProductId = UUID.randomUUID()
    private val boxUnitId = UUID.randomUUID()
    private val unitNameByUnitId = mapOf(boxUnitId to "packet")

    private fun saleLine(quantity: String): SaleSessionLine = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = locationProductId,
        productLabel = "Cement",
        productPrintLabel = "Cement",
        userProvidedQuantity = BigDecimal(quantity),
        unitId = boxUnitId,
        baseUnitId = boxUnitId,
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal(quantity).multiply(BigDecimal("10.00")),
        defaultSalePrice = BigDecimal("10.00"),
    )

    private fun storyFor(kind: ConsolidationProblemKind) = storyFor(kind, saleLine("2"), saleLine("3.5"))

    private fun storyFor(kind: ConsolidationProblemKind, firstLine: SaleSessionLine, secondLine: SaleSessionLine) =
        ConsolidationProblemStoryWriter.write(
            consolidationProblems = listOf(
                ConsolidationProblem(locationProductId, kind, listOf(firstLine.identity, secondLine.identity))
            ),
            problemSaleLines = listOf(firstLine, secondLine),
            problemSaleAdjustments = emptyList(),
            sourceSaleReferenceByLineKey = mapOf(firstLine.lineKey to "S2", secondLine.lineKey to "S1"),
            unitNameByUnitId = unitNameByUnitId,
        ).single()

    @Test
    fun `every kind names the product, both sales and the lines`() {
        ConsolidationProblemKind.entries.forEach { kind ->
            val blockingProblem = storyFor(kind)
            assertEquals("S2, S1", blockingProblem.documentReference, kind.name)
            assertTrue(blockingProblem.description.contains("Cement"), kind.name)
            val priceSource = if (kind == ConsolidationProblemKind.OVERRIDE_ON_SOME_LINES_ONLY) " (normal price)" else ""
            val expectedLinesText = "Lines: 2 packets at KES 10.00 per packet$priceSource from S2; " +
                "3.5 packets at KES 10.00 per packet$priceSource from S1."
            assertTrue(blockingProblem.effect.startsWith(expectedLinesText), kind.name)
        }
    }

    @Test
    fun `every kind has its own description and its own fix`() {
        val storyByKind = ConsolidationProblemKind.entries.associateWith { storyFor(it) }
        assertEquals(ConsolidationProblemKind.entries.size, storyByKind.values.map { it.description }.distinct().size)
        assertEquals(ConsolidationProblemKind.entries.size, storyByKind.values.map { it.effect }.distinct().size)
    }

    @Test
    fun `quantity limits come from the quantity limits constants`() {
        val tooPreciseDescription = storyFor(ConsolidationProblemKind.QUANTITY_TOO_PRECISE).description
        val tooLargeDescription = storyFor(ConsolidationProblemKind.QUANTITY_TOO_LARGE).description
        assertTrue(tooPreciseDescription.contains("more than 2 decimal places"))
        assertTrue(tooLargeDescription.contains("999,999,999,999.99"))
    }

    @Test
    fun `a sale that holds two lines of the product is named once`() {
        val firstLine = saleLine("1")
        val secondLine = saleLine("2")
        val blockingProblem = ConsolidationProblemStoryWriter.write(
            consolidationProblems = listOf(
                ConsolidationProblem(
                    locationProductId, ConsolidationProblemKind.TYPED_PRICES_DIFFER,
                    listOf(firstLine.identity, secondLine.identity)
                )
            ),
            problemSaleLines = listOf(firstLine, secondLine),
            problemSaleAdjustments = emptyList(),
            sourceSaleReferenceByLineKey = mapOf(firstLine.lineKey to "S1", secondLine.lineKey to "S1"),
            unitNameByUnitId = unitNameByUnitId,
        ).single()
        assertEquals("S1", blockingProblem.documentReference)
    }

    @Test
    fun `problems come back ordered by sale reference`() {
        val laterLine = saleLine("1")
        val earlierLine = saleLine("1")
        val blockingProblems = ConsolidationProblemStoryWriter.write(
            consolidationProblems = listOf(
                ConsolidationProblem(
                    locationProductId, ConsolidationProblemKind.INACTIVE_PRODUCT, listOf(laterLine.identity)
                ),
                ConsolidationProblem(
                    locationProductId, ConsolidationProblemKind.INACTIVE_PRODUCT, listOf(earlierLine.identity)
                ),
            ),
            problemSaleLines = listOf(laterLine, earlierLine),
            problemSaleAdjustments = emptyList(),
            sourceSaleReferenceByLineKey = mapOf(laterLine.lineKey to "S9", earlierLine.lineKey to "S3"),
            unitNameByUnitId = unitNameByUnitId,
        )
        assertEquals(listOf("S3", "S9"), blockingProblems.map { it.documentReference })
    }
}
