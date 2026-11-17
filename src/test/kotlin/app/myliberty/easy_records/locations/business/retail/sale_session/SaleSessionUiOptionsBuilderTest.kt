package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale_session.api.PendingConsolidation
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleDiscardAction
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionKind
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionPayment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionTotals
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.util.enums.SystemContact
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class SaleSessionUiOptionsBuilderTest {

    private val sugarId = UUID.randomUUID()

    private fun build(saleSession: SaleSession, inactiveLocationProductIds: Set<UUID> = emptySet()) =
        SaleSessionUiOptionsBuilder.build(
            saleSession = saleSession,
            inactiveLocationProductIds = inactiveLocationProductIds,
            showActiveUserWarning = false,
            canGenerateConsolidatedSale = false,
        )

    @Test
    fun `a draft sale can do everything and has nothing blocking it`() {
        val uiOptions = build(session(lines = listOf(line())))

        assertTrue(uiOptions.canAddLines)
        assertTrue(uiOptions.canEditLines)
        assertTrue(uiOptions.canChangeContact)
        assertTrue(uiOptions.canSaveDraft)
        assertTrue(uiOptions.canConfirm)
        assertTrue(uiOptions.saveDraftBlockers.isEmpty())
        assertTrue(uiOptions.confirmBlockers.isEmpty())
        assertEquals(SaleDiscardAction.DISCARD_DRAFT, uiOptions.allowedDiscardAction)
    }

    @Test
    fun `a pending consolidation edits lines and nothing else, and can only be discarded`() {
        val uiOptions = build(pendingSession())

        assertFalse(uiOptions.canAddLines)
        assertTrue(uiOptions.canEditLines)
        assertFalse(uiOptions.canChangeContact)
        assertFalse(uiOptions.canSaveDraft)
        assertFalse(uiOptions.canConfirm)
        assertFalse(uiOptions.canAddPaymentsToSale)
        assertFalse(uiOptions.canCopySale)
        assertEquals(SaleDiscardAction.DISCARD_PENDING_CONSOLIDATION, uiOptions.allowedDiscardAction)
    }

    @Test
    fun `a confirmed sale is read-only and is voided only once its payments are`() {
        val payment = payment(voidedReason = null)
        val voidedPayment = payment(voidedReason = "wrong amount")

        val withActivePayment = build(session(status = SaleStatus.CONFIRMED, payments = listOf(payment)))
        val withVoidedPayment = build(session(status = SaleStatus.CONFIRMED, payments = listOf(voidedPayment)))

        assertFalse(withActivePayment.canEditLines)
        assertFalse(withActivePayment.canSaveDraft)
        assertNull(withActivePayment.allowedDiscardAction)
        assertEquals(SaleDiscardAction.VOID_SALE, withVoidedPayment.allowedDiscardAction)
    }

    @Test
    fun `a voided or discarded sale cannot be discarded again`() {
        assertNull(build(session(status = SaleStatus.VOIDED)).allowedDiscardAction)
        assertNull(build(session(status = SaleStatus.DISCARDED)).allowedDiscardAction)
    }

    @Test
    fun `only a saved sale that is not confirmed can be copied`() {
        assertFalse(build(session(saleId = null)).canCopySale)
        assertTrue(build(session(saleId = UUID.randomUUID())).canCopySale)
        assertFalse(build(session(saleId = UUID.randomUUID(), status = SaleStatus.CONFIRMED)).canCopySale)
    }

    @Test
    fun `confirming is blocked by an empty sale, an unpaid walk-in and payments above the total`() {
        val emptySale = build(session(lines = emptyList()))
        val unpaidWalkIn = build(
            session(contactId = SystemContact.WALK_IN.id, lines = listOf(line()), totals = totalsOfTenPaid("0"))
        )
        val overpaid = build(session(lines = listOf(line()), totals = totalsOfTenPaid("12")))

        assertEquals(listOf("Sale must have at least one line"), emptySale.confirmBlockers)
        assertEquals(listOf("Walk-in customers must pay in full"), unpaidWalkIn.confirmBlockers)
        assertEquals(listOf("Total paid cannot exceed order total"), overpaid.confirmBlockers)
        assertTrue(emptySale.saveDraftBlockers.isEmpty())
    }

    @Test
    fun `every confirm blocker on the session is also refused by the confirm guard`() {
        val saleSessionValidator = SaleSessionValidator(
            mock(AdjustmentReasonService::class.java), mock(LocationProductService::class.java)
        )
        val emptySale = session(lines = emptyList())
        val unpaidWalkIn = session(
            contactId = SystemContact.WALK_IN.id, lines = listOf(line()), totals = totalsOfTenPaid("0")
        )
        val overpaid = session(lines = listOf(line()), totals = totalsOfTenPaid("12"))

        assertThrows(RtsGenericException::class.java) { saleSessionValidator.guardNonEmptyLines(emptySale) }
        assertThrows(RtsGenericException::class.java) { saleSessionValidator.guardWalkInFullyCovered(unpaidWalkIn) }
        assertThrows(RtsGenericException::class.java) { saleSessionValidator.guardPaymentsWithinTotal(overpaid) }
        listOf(emptySale, unpaidWalkIn, overpaid).forEach { blockedSession ->
            assertFalse(build(blockedSession).confirmBlockers.isEmpty())
        }
    }

    @Test
    fun `an inactive product blocks both saving and confirming and is named`() {
        val uiOptions = build(session(lines = listOf(line())), inactiveLocationProductIds = setOf(sugarId))

        assertEquals(listOf("Sugar is no longer active"), uiOptions.saveDraftBlockers)
        assertEquals(listOf("Sugar is no longer active"), uiOptions.confirmBlockers)
    }

    private fun totalsOfTenPaid(paymentTotal: String) = SaleSessionTotals.ZERO.copy(
        receivableTotal = BigDecimal("10"),
        paymentTotal = BigDecimal(paymentTotal),
    )

    private fun payment(voidedReason: String?) = SaleSessionPayment(
        SessionIdentity.mintFreshIdentity(), UUID.randomUUID(), BigDecimal.ONE, null, null, voidedReason
    )

    private fun line() = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = sugarId,
        productLabel = "Sugar",
        productPrintLabel = "Sugar",
        userProvidedQuantity = BigDecimal.ONE,
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal("10.00"),
        defaultSalePrice = BigDecimal("10.00"),
    )

    private fun pendingSession() = session(lines = listOf(line()), saleId = null).copy(
        kind = SaleSessionKind.PENDING_CONSOLIDATION,
        pendingConsolidation = PendingConsolidation(emptyList(), emptyMap()),
    )

    private fun session(
        lines: List<SaleSessionLine> = emptyList(),
        status: SaleStatus = SaleStatus.DRAFT,
        payments: List<SaleSessionPayment> = emptyList(),
        saleId: UUID? = null,
        contactId: UUID = UUID.randomUUID(),
        totals: SaleSessionTotals = SaleSessionTotals.ZERO,
    ) = SaleSession(
        sessionId = UUID.randomUUID(),
        locationId = UUID.randomUUID(),
        saleId = saleId,
        saleVersion = null,
        productsVersion = 0L,
        productsReservedAtVersion = 0L,
        originalStatus = status,
        createdById = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        lastUpdatedAt = OffsetDateTime.now(),
        lastAccessedById = UUID.randomUUID(),
        lastAccessedAt = OffsetDateTime.now(),
        header = SaleSessionHeader(
            referenceNumber = null, contactId = contactId, soldById = null, dateSold = null, notes = null
        ),
        saleLines = lines,
        saleAdjustments = emptyList(),
        salePayments = payments,
        totals = totals,
    )
}
