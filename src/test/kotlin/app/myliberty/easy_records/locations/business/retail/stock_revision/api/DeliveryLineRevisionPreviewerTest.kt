package app.myliberty.easy_records.locations.business.retail.stock_revision.api

import app.myliberty.easy_records.locations.business.retail.delivery.api.ValidatedDeliveryLineRevisionDto
import app.myliberty.easy_records.locations.business.retail.stock_revision.DeliveryLineRevisionAssessment
import app.myliberty.easy_records.locations.business.retail.stock_revision.DeliveryLineRevisionAssessor
import app.myliberty.easy_records.locations.business.retail.stock_revision.AdjustmentCostChangeResolver
import app.myliberty.easy_records.locations.business.retail.stock_revision.ReplayResult
import app.myliberty.easy_records.locations.business.retail.stock_revision.ReservationShortfall
import app.myliberty.easy_records.locations.business.retail.stock_revision.RevisionLedgerPreviewWriter
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.BlockingProblem
import app.myliberty.easy_records.util.quantity.BreakdownUnit
import app.myliberty.easy_records.util.quantity.ExactQuantity
import app.myliberty.easy_records.util.quantity.QuantityBreakdown
import app.myliberty.easy_records.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class DeliveryLineRevisionPreviewerTest {

    private val unitId = UUID.randomUUID()
    private val revisedDeliveryLineId = UUID.randomUUID()
    private val quantityBreakdown = QuantityBreakdown.forBaseUnit(
        listOf(BreakdownUnit(unitId, "pcs", 1L, unitId, OffsetDateTime.parse("2026-11-01T10:00:00Z"))), unitId
    )
    private val deliveryLineRevisionAssessor = mock(DeliveryLineRevisionAssessor::class.java)
    private val revisionLedgerPreviewWriter = mock(RevisionLedgerPreviewWriter::class.java)
    private val adjustmentCostChangeResolver = mock(AdjustmentCostChangeResolver::class.java)
    private val deliveryLineRevisionPreviewer = DeliveryLineRevisionPreviewer(
        deliveryLineRevisionAssessor, revisionLedgerPreviewWriter, adjustmentCostChangeResolver
    )

    private val deliveryLineRevisionRequest = DeliveryLineRevisionRequest(
        revisedDeliveryLineId = revisedDeliveryLineId,
        userProvidedQuantityDelivered = UserProvidedQuantity.of(BigDecimal("8")),
        unitId = unitId,
        unitCost = Money.of(BigDecimal("12.0000")),
        reason = "Miscounted",
    )

    private fun givenAssessment(
        validationProblems: List<BlockingProblem> = emptyList(),
        replayProblems: List<BlockingProblem> = emptyList(),
        reservationProblems: List<BlockingProblem> = emptyList()
    ) {
        `when`(deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)).thenReturn(
            DeliveryLineRevisionAssessment(
                ValidatedDeliveryLineRevisionDto(
                    revisedDeliveryLineId = revisedDeliveryLineId,
                    deliveryReferenceNumber = "DEL000001",
                    revisedLineReferenceNumber = "DELN000001",
                    purchaseLineId = UUID.randomUUID(),
                    locationProductId = UUID.randomUUID(),
                    revisedUnitId = unitId,
                    revisedStorageUnitsPerUserProvidedUnit = 1L,
                    unitId = unitId,
                    storageUnitsPerUserProvidedUnit = 1L,
                    revisedUserProvidedQuantityDelivered = BigDecimal("10"),
                    revisedUnitCost = BigDecimal("10.0000"),
                    userProvidedQuantityDelivered = BigDecimal("8"),
                    unitCost = BigDecimal("12.0000"),
                    problems = validationProblems,
                ),
                quantityBreakdown,
                ReplayResult(emptyList(), replayProblems),
                reservationProblems.takeIf { it.isNotEmpty() }?.let {
                    ReservationShortfall(it, "Sugar", ExactQuantity.ZERO, ExactQuantity.ZERO)
                }
            )
        )
    }

    @Test
    fun `a failed check is shown as a blocking row`() {
        givenAssessment(validationProblems = listOf(BlockingProblem("DELN000001", "Already changed.", "Change it.")))

        val preview = deliveryLineRevisionPreviewer.preview(deliveryLineRevisionRequest)

        assertEquals(listOf("DELN000001"), preview.transactions.map { it.documentReference })
        assertTrue(preview.blocksSave)
    }

    @Test
    fun `a revision with nothing affected has a summary and no rows`() {
        givenAssessment()

        val preview = deliveryLineRevisionPreviewer.preview(deliveryLineRevisionRequest)

        assertTrue(preview.transactions.isEmpty())
        assertTrue(preview.changeSummary.contains(" becomes "))
    }

    @Test
    fun `ledger entries are previewed only when nothing blocks the save`() {
        givenAssessment()

        deliveryLineRevisionPreviewer.preview(deliveryLineRevisionRequest)

        assertEquals(1, mockingDetails(revisionLedgerPreviewWriter).invocations.size)
    }

    @Test
    fun `no ledger entries are previewed while a check blocks the save`() {
        givenAssessment(replayProblems = listOf(BlockingProblem("SAL1", "Sale that took 4 pcs.", "Enter more.")))

        val preview = deliveryLineRevisionPreviewer.preview(deliveryLineRevisionRequest)

        assertTrue(preview.ledgerEntries.isEmpty())
        verifyNoInteractions(revisionLedgerPreviewWriter)
    }

    @Test
    fun `short reservations are blocking rows`() {
        givenAssessment(
            reservationProblems = listOf(
                BlockingProblem("SAL000420 (draft)", "Draft sale holding 4 pcs.", "Remove it.")
            )
        )

        val preview = deliveryLineRevisionPreviewer.preview(deliveryLineRevisionRequest)

        assertEquals(listOf("SAL000420 (draft)"), preview.transactions.map { it.documentReference })
        assertTrue(preview.blocksSave)
    }

    @Test
    fun `a replay shortfall is a blocking row`() {
        givenAssessment(replayProblems = listOf(BlockingProblem("SAL1", "Sale that took 4 pcs.", "Enter more.")))

        val preview = deliveryLineRevisionPreviewer.preview(deliveryLineRevisionRequest)

        assertEquals(listOf("SAL1"), preview.transactions.map { it.documentReference })
    }
}
