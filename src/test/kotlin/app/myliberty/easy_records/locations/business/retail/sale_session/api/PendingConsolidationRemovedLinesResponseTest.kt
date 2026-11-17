package app.myliberty.easy_records.locations.business.retail.sale_session.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class PendingConsolidationRemovedLinesResponseTest {

    private val removedLine = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = UUID.randomUUID(),
        productLabel = "Sugar",
        productPrintLabel = "Sugar",
        userProvidedQuantity = BigDecimal("5"),
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal("75.00"),
        defaultSalePrice = BigDecimal("15.00"),
    )

    @Test
    fun `a removed line is shown with its product, quantity, price, total and source sale`() {
        val pendingConsolidation = PendingConsolidation(
            sourceSales = emptyList(),
            sourceSaleReferenceByLineKey = mapOf(removedLine.lineKey to "S2"),
            removedSaleLines = listOf(RemovedSaleLine(removedLine, emptyList())),
        )

        val response = pendingConsolidation.removedSaleLineResponses().single()

        assertEquals(removedLine.identity, response.identity)
        assertEquals("Sugar", response.productLabel)
        assertEquals(BigDecimal("5"), response.userProvidedQuantity)
        assertEquals(BigDecimal("15.00"), response.unitPrice)
        assertEquals(BigDecimal("75.00"), response.lineTotal)
        assertEquals("S2", response.sourceSaleReference)
    }

    @Test
    fun `nothing removed gives an empty list`() {
        assertTrue(PendingConsolidation(emptyList(), emptyMap()).removedSaleLineResponses().isEmpty())
    }
}
