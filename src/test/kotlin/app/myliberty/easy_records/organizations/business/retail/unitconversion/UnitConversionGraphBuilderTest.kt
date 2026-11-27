package app.myliberty.easy_records.organizations.business.retail.unitconversion

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.retail.unitgroup.api.UnitGroupDataFetcher
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueFetcher
import app.myliberty.easy_records.organizations.business.retail.unitvalue.api.UnitValueResponseDto
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class UnitConversionGraphBuilderTest {

    private val miscellaneousGroupId = UUID.randomUUID()
    private val countableGroupId = UUID.randomUUID()
    private val weightGroupId = UUID.randomUUID()

    private val pieceId = UUID.randomUUID()
    private val dozenId = UUID.randomUUID()
    private val gramId = UUID.randomUUID()
    private val miscellaneousPieceId = UUID.randomUUID()
    private val otherMiscellaneousPieceId = UUID.randomUUID()

    private val createdOn: OffsetDateTime = OffsetDateTime.parse("2026-01-01T00:00:00Z")

    private val unitValueFetcher: UnitValueFetcher = mock(UnitValueFetcher::class.java)
    private val unitGroupDataFetcher: UnitGroupDataFetcher = mock(UnitGroupDataFetcher::class.java)

    @BeforeEach
    fun setOrganizationSession() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
        `when`(unitGroupDataFetcher.isMiscellaneous(miscellaneousGroupId)).thenReturn(true)
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a Miscellaneous unit converts only to itself, not to the other units of its group`() {
        val graphBuilder = graphBuilderOver(
            unitValueOf(miscellaneousPieceId, "Piece", "misc-pc", miscellaneousGroupId, baseUnit = null),
            unitValueOf(otherMiscellaneousPieceId, "Other Piece", "misc-two", miscellaneousGroupId, baseUnit = null)
        )

        val fullGraph = graphBuilder.getOrLoad().getFullGraph()

        assertEquals(setOf(miscellaneousPieceId), fullGraph.getValue(miscellaneousPieceId).keys)
        assertEquals(setOf(otherMiscellaneousPieceId), fullGraph.getValue(otherMiscellaneousPieceId).keys)
    }

    @Test
    fun `units convert within their own group and not to a unit of another group`() {
        val graphBuilder = graphBuilderOver(
            unitValueOf(pieceId, "Piece", "pc", countableGroupId, baseUnit = null),
            unitValueOf(dozenId, "Dozen", "dz", countableGroupId, baseUnit = pieceId, unitsOfBasePerUnit = 12),
            unitValueOf(gramId, "Gram", "g", weightGroupId, baseUnit = null)
        )

        val graph = graphBuilder.getOrLoad()

        assertEquals(setOf(pieceId, dozenId), graph.getFullGraph().getValue(dozenId).keys)
        assertEquals(setOf(gramId), graph.getFullGraph().getValue(gramId).keys)
        val exception = assertThrows(LibertyGenericException::class.java) { graph.getTarget(dozenId, gramId) }
        assertEquals("No conversion target found from Dozen to Gram", exception.message)
    }

    @Test
    fun `each target carries the target unit's identity and the size ratio in lowest terms`() {
        val graphBuilder = graphBuilderOver(
            unitValueOf(pieceId, "Piece", "pc", countableGroupId, baseUnit = null),
            unitValueOf(dozenId, "Dozen", "dz", countableGroupId, baseUnit = pieceId, unitsOfBasePerUnit = 12)
        )

        val fullGraph = graphBuilder.getOrLoad().getFullGraph()

        val dozenToPiece = fullGraph.getValue(dozenId).getValue(pieceId)
        assertEquals(pieceId, dozenToPiece.id)
        assertEquals("Piece", dozenToPiece.name)
        assertEquals("pc", dozenToPiece.code)
        assertEquals(12L, dozenToPiece.numerator)
        assertEquals(1L, dozenToPiece.denominator)
        val pieceToDozen = fullGraph.getValue(pieceId).getValue(dozenId)
        assertEquals("Dozen", pieceToDozen.name)
        assertEquals(1L, pieceToDozen.numerator)
        assertEquals(12L, pieceToDozen.denominator)
    }

    private fun graphBuilderOver(vararg unitValues: UnitValueResponseDto): UnitConversionGraphBuilder {
        `when`(unitValueFetcher.getAllUnitValues()).thenReturn(unitValues.toList())
        return UnitConversionGraphBuilder(unitValueFetcher, unitGroupDataFetcher)
    }

    private fun unitValueOf(
        id: UUID,
        name: String,
        code: String,
        unitGroupId: UUID,
        baseUnit: UUID?,
        unitsOfBasePerUnit: Long? = null
    ) = UnitValueResponseDto(
        id = id,
        name = name,
        code = code,
        description = null,
        baseUnit = baseUnit,
        baseUnitName = null,
        unitsOfBasePerUnit = unitsOfBasePerUnit,
        createdBy = "test",
        createdOn = createdOn,
        unitGroupId = unitGroupId,
        referenceNumber = "UV000001",
        systemDefined = false
    )
}
