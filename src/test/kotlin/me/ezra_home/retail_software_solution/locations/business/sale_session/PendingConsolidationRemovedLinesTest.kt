package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.PendingConsolidation
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.RemovedSaleLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionKind
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PendingConsolidationRemovedLinesTest {

    private val firstLine = saleLine("2")
    private val secondLine = saleLine("3")
    private val firstLineDiscount = adjustmentOn(firstLine)

    @Test
    fun `removing a line from a pending consolidation keeps it with its adjustments`() {
        val pendingSession = session(SaleSessionKind.PENDING_CONSOLIDATION)

        val afterRemoval = PendingConsolidationRemovedLines.remove(pendingSession, firstLine.lineKey)

        assertEquals(listOf(secondLine), afterRemoval.saleLines)
        assertTrue(afterRemoval.saleAdjustments.isEmpty())
        assertEquals(
            listOf(RemovedSaleLine(firstLine, listOf(firstLineDiscount))),
            afterRemoval.pendingConsolidation!!.removedSaleLines
        )
        assertEquals("S1", afterRemoval.pendingConsolidation.sourceSaleReferenceByLineKey.getValue(firstLine.lineKey))
    }

    @Test
    fun `a restored line comes back with its adjustments and leaves the removed list`() {
        val pendingSession = session(SaleSessionKind.PENDING_CONSOLIDATION)
        val afterRemoval = PendingConsolidationRemovedLines.remove(pendingSession, firstLine.lineKey)

        val afterRestore = PendingConsolidationRemovedLines.restore(afterRemoval, firstLine.lineKey)

        assertEquals(setOf(firstLine, secondLine), afterRestore.saleLines.toSet())
        assertEquals(firstLine.rank, afterRestore.saleLines.single { it.lineKey == firstLine.lineKey }.rank)
        assertEquals(listOf(firstLineDiscount), afterRestore.saleAdjustments)
        assertTrue(afterRestore.pendingConsolidation!!.removedSaleLines.isEmpty())
    }

    @Test
    fun `a line that is not on the session cannot be removed, and one that was not removed cannot be restored`() {
        val pendingSession = session(SaleSessionKind.PENDING_CONSOLIDATION)

        assertThrows(RtsGenericException::class.java) {
            PendingConsolidationRemovedLines.remove(pendingSession, UUID.randomUUID())
        }
        assertThrows(RtsGenericException::class.java) {
            PendingConsolidationRemovedLines.restore(pendingSession, firstLine.lineKey)
        }
    }

    @Test
    fun `a sale has no removed lines to keep or restore`() {
        val saleSession = session(SaleSessionKind.SALE)

        assertThrows(RtsGenericException::class.java) {
            PendingConsolidationRemovedLines.remove(saleSession, firstLine.lineKey)
        }
        assertThrows(RtsGenericException::class.java) {
            PendingConsolidationRemovedLines.restore(saleSession, firstLine.lineKey)
        }
    }

    @Test
    fun `removing a line from a sale deletes it and its adjustments`() {
        val afterRemoval = session(SaleSessionKind.SALE).withoutLine(firstLine.lineKey)

        assertEquals(listOf(secondLine), afterRemoval.saleLines)
        assertTrue(afterRemoval.saleAdjustments.isEmpty())
        assertEquals(null, afterRemoval.pendingConsolidation)
    }

    @Test
    fun `a line that is not on a sale cannot be removed from it`() {
        assertThrows(RtsGenericException::class.java) {
            session(SaleSessionKind.SALE).withoutLine(UUID.randomUUID())
        }
    }

    private fun saleLine(quantity: String) = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = UUID.randomUUID(),
        productLabel = "Sugar",
        productPrintLabel = "Sugar",
        userProvidedQuantity = BigDecimal(quantity),
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal("30.00"),
        defaultSalePrice = BigDecimal("15.00"),
    )

    private fun adjustmentOn(saleLine: SaleSessionLine) = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = saleLine.identity,
        adjustmentReasonId = UUID.randomUUID(),
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
        value = BigDecimal("5"),
        note = null,
        approvedById = null,
    )

    private fun session(kind: SaleSessionKind) = SaleSession(
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
        saleLines = listOf(firstLine, secondLine),
        saleAdjustments = listOf(firstLineDiscount),
        salePayments = emptyList(),
        totals = SaleSessionTotals.ZERO,
        kind = kind,
        pendingConsolidation = if (kind == SaleSessionKind.PENDING_CONSOLIDATION) {
            PendingConsolidation(
                sourceSales = emptyList(),
                sourceSaleReferenceByLineKey = mapOf(firstLine.lineKey to "S1", secondLine.lineKey to "S2"),
            )
        } else {
            null
        },
    )
}
