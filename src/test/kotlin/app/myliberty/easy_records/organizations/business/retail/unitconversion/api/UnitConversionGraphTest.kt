package app.myliberty.easy_records.organizations.business.retail.unitconversion.api

import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitPlacement
import app.myliberty.easy_records.util.business.ConversionRatio
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class UnitConversionGraphTest {

    private val pieceId = UUID.randomUUID()
    private val dozenId = UUID.randomUUID()
    private val gramId = UUID.randomUUID()
    private val unknownId = UUID.randomUUID()

    private val breakdownUnits = listOf(
        BreakdownUnit(pieceId, "Piece", 1, pieceId, OffsetDateTime.parse("2030-01-01T00:00:00Z"))
    )

    private val graph = UnitConversionGraph(
        mapOf(
            pieceId to mapOf(
                pieceId to ConversionTargetDto(pieceId, "Piece", "pc", 1, 1),
                dozenId to ConversionTargetDto(dozenId, "Dozen", "dz", 1, 12)
            ),
            dozenId to mapOf(
                pieceId to ConversionTargetDto(pieceId, "Piece", "pc", 12, 1),
                dozenId to ConversionTargetDto(dozenId, "Dozen", "dz", 1, 1)
            ),
            gramId to mapOf(gramId to ConversionTargetDto(gramId, "Gram", "g", 1, 1))
        ),
        mapOf(pieceId to "Piece", dozenId to "Dozen", gramId to "Gram"),
        mapOf(pieceId to UnitPlacement(pieceId, 1), dozenId to UnitPlacement(pieceId, 12)),
        breakdownUnits
    )

    @Test
    fun `a unit's storage unit and size are read from its placement in the group root`() {
        assertEquals(pieceId, graph.getStorageUnitId(dozenId))
        assertEquals(12L, graph.getSizeInRoot(dozenId))
        assertEquals(1L, graph.getSizeInRoot(pieceId))
    }

    @Test
    fun `the placement of a unit the graph does not know is refused`() {
        val storageException = assertThrows(LibertyGenericException::class.java) { graph.getStorageUnitId(unknownId) }
        val sizeException = assertThrows(LibertyGenericException::class.java) { graph.getSizeInRoot(unknownId) }

        assertEquals("Unit not found for id $unknownId", storageException.message)
        assertEquals("Unit not found for id $unknownId", sizeException.message)
    }

    @Test
    fun `the breakdown units are the ones the graph was built with`() {
        assertSame(breakdownUnits, graph.getBreakdownUnits())
    }

    @Test
    fun `the ratio between two units is the target's numerator over its denominator`() {
        assertEquals(ConversionRatio(12, 1), graph.getRatio(dozenId, pieceId))
        assertEquals(ConversionRatio(1, 12), graph.getRatio(pieceId, dozenId))
    }

    @Test
    fun `the ratio of a unit to itself is one to one without consulting the graph`() {
        assertSame(ConversionRatio.IDENTITY, graph.getRatio(unknownId, unknownId))
    }

    @Test
    fun `a ratio from a unit that has no targets is refused naming the unit`() {
        val graphWithoutPieceTargets = UnitConversionGraph(emptyMap(), mapOf(pieceId to "Piece", dozenId to "Dozen"))

        val exception = assertThrows(LibertyGenericException::class.java) {
            graphWithoutPieceTargets.getRatio(pieceId, dozenId)
        }

        assertEquals("No conversion targets found for unit Piece", exception.message)
    }

    @Test
    fun `a ratio to a unit outside the group is refused naming both units`() {
        val exception = assertThrows(LibertyGenericException::class.java) { graph.getRatio(dozenId, gramId) }

        assertEquals("No conversion target found from Dozen to Gram", exception.message)
    }

    @Test
    fun `a ratio naming a unit the graph has no name for is refused`() {
        val exception = assertThrows(LibertyGenericException::class.java) { graph.getRatio(unknownId, pieceId) }

        assertEquals("Unit name not found for id $unknownId", exception.message)
    }
}
