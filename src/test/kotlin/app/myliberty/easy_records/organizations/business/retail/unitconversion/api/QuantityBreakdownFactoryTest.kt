package app.myliberty.easy_records.organizations.business.retail.unitconversion.api

import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitPlacement
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import app.myliberty.easy_records.util.quantity.ExactQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class QuantityBreakdownFactoryTest {

    private val pieceId = UUID.randomUUID()
    private val dozenId = UUID.randomUUID()
    private val createdOn = OffsetDateTime.parse("2030-01-01T00:00:00Z")

    private val unitConversionGraphFacade: UnitConversionGraphFacade = Mockito.mock(UnitConversionGraphFacade::class.java)
    private val quantityBreakdownFactory = QuantityBreakdownFactory(unitConversionGraphFacade)

    @Test
    fun `no base units gives no breakdowns and never loads the graph`() {
        assertTrue(quantityBreakdownFactory.forBaseUnits(emptyList()).isEmpty())

        Mockito.verifyNoInteractions(unitConversionGraphFacade)
    }

    @Test
    fun `each distinct base unit gets one breakdown built from the graph's units`() {
        val graph = UnitConversionGraph(
            emptyMap(),
            emptyMap(),
            mapOf(pieceId to UnitPlacement(pieceId, 1), dozenId to UnitPlacement(pieceId, 12)),
            listOf(
                BreakdownUnit(pieceId, "Piece", 1, pieceId, createdOn),
                BreakdownUnit(dozenId, "Dozen", 12, pieceId, createdOn)
            )
        )
        Mockito.`when`(unitConversionGraphFacade.getOrLoad()).thenReturn(graph)

        val breakdownsByBaseUnitId = quantityBreakdownFactory.forBaseUnits(listOf(dozenId, pieceId, dozenId))

        assertEquals(setOf(dozenId, pieceId), breakdownsByBaseUnitId.keys)
        assertEquals("2 Dozens 1 Piece", breakdownsByBaseUnitId.getValue(dozenId).render(ExactQuantity.ofStorage(BigDecimal(25))))
        assertEquals("25 Pieces", breakdownsByBaseUnitId.getValue(pieceId).render(ExactQuantity.ofStorage(BigDecimal(25))))
        Mockito.verify(unitConversionGraphFacade).getOrLoad()
    }
}
