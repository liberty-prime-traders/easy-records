package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.PendingConsolidation
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAction
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionKind
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionPayment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.SystemAdjustmentReason
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.business.Decimals
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@WithDefaultOrganizationSession
class SaleSessionValidatorTest {

    private val adjustmentReasonService = mock(AdjustmentReasonService::class.java)
    private val locationProductService = mock(LocationProductService::class.java)
    private val saleSessionValidator = SaleSessionValidator(adjustmentReasonService, locationProductService)
    private val maximum = Decimals.MAXIMUM_TRANSACTION_AMOUNT

    private val priceOverrideReasonId = UUID.randomUUID()

    init {
        `when`(adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)).thenReturn(priceOverrideReasonId)
    }

    @Test
    fun `only draft and confirmed sales can be consolidated, and never a walk-in sale`() {
        val contactId = UUID.randomUUID()
        saleSessionValidator.guardSourceSalesConsolidatable(
            listOf(
                consolidationHeader(SaleStatus.DRAFT, contactId),
                consolidationHeader(SaleStatus.CONFIRMED, contactId),
            )
        )

        listOf(SaleStatus.VOIDED, SaleStatus.DISCARDED).forEach { refusedStatus ->
            val refusal = assertThrows(RtsGenericException::class.java) {
                saleSessionValidator.guardSourceSalesConsolidatable(
                    listOf(
                        consolidationHeader(SaleStatus.DRAFT, contactId),
                        consolidationHeader(refusedStatus, contactId),
                    )
                )
            }
            assertTrue(refusal.message.contains("SALE-$refusedStatus"))
        }
        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.guardSourceSalesConsolidatable(
                listOf(consolidationHeader(SaleStatus.DRAFT, SystemContact.WALK_IN.id))
            )
        }
    }

    private fun consolidationHeader(status: SaleStatus, contactId: UUID) = SaleHeaderDto(
        id = UUID.randomUUID(), referenceNumber = "SALE-$status", version = 0, status = status,
        contactId = contactId, soldById = null, dateSold = null, notes = null,
    )

    @Test
    fun `the system price override reason is not accepted for an added adjustment, any other reason is`() {
        assertThrows(RtsGenericException::class.java) { saleSessionValidator.guardReasonIsNotPriceOverride(priceOverrideReasonId) }

        saleSessionValidator.guardReasonIsNotPriceOverride(UUID.randomUUID())
    }

    @Test
    fun `a session whose amounts sit exactly at the maximum is accepted`() {
        saleSessionValidator.validate(saleSession(listOf(saleSessionLine(maximum))))
    }

    @Test
    fun `a line total above the maximum is rejected before the database column can overflow`() {
        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.validate(saleSession(listOf(saleSessionLine(maximum + BigDecimal("0.01")))))
        }
    }

    @Test
    fun `lines that are each within the maximum are rejected when their subtotal is not`() {
        val saleSession = saleSession(listOf(saleSessionLine(BigDecimal("600000000000.00")), saleSessionLine(BigDecimal("600000000000.00"))))

        assertThrows(RtsGenericException::class.java) { saleSessionValidator.validate(saleSession) }
    }

    @Test
    fun `a price override whose price times quantity is huge is rejected even though a surcharge has no ceiling`() {
        val line = saleSessionLine(BigDecimal("10.00"), quantity = BigDecimal("1000"))
        val surcharge = priceOverrideAdjustment(line, maximum)

        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.validate(saleSession(listOf(line), listOf(surcharge)))
        }
    }

    @Test
    fun `a discount whose amount is above the maximum is rejected even though the subtotal and receivable total are within it`() {
        val line = saleSessionLine(maximum)
        val discount = percentageAdjustment(AdjustmentDirection.DISCOUNT, BigDecimal("200"), line)

        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.validate(saleSession(listOf(line), listOf(discount)))
        }
    }

    @Test
    fun `a surcharge that pushes the receivable total above the maximum is rejected while the subtotal and the adjustment are within it`() {
        val surcharge = percentageAdjustment(AdjustmentDirection.SURCHARGE, BigDecimal("10"), null)

        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.validate(saleSession(listOf(saleSessionLine(maximum)), listOf(surcharge)))
        }
    }

    @Test
    fun `a session line whose product is inactive blocks the commit until the line is removed`() {
        val inactiveProductLine = saleSessionLine(BigDecimal("10.00"))
        val activeProductLine = saleSessionLine(BigDecimal("5.00"))
        doThrow(RtsGenericException("Inactive products are not allowed"))
            .`when`(locationProductService).guardAllActive(listOf(inactiveProductLine.locationProductId, activeProductLine.locationProductId))

        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.guardNoInactiveProducts(saleSession(listOf(inactiveProductLine, activeProductLine)))
        }

        saleSessionValidator.guardNoInactiveProducts(saleSession(listOf(activeProductLine)))
    }

    @Test
    fun `only a confirmed sale is refused as the source of a copy`() {
        assertThrows(RtsGenericException::class.java) { saleSessionValidator.guardSourceSaleCopyable(SaleStatus.CONFIRMED) }

        listOf(SaleStatus.DRAFT, SaleStatus.VOIDED, SaleStatus.DISCARDED).forEach { copyableStatus ->
            saleSessionValidator.guardSourceSaleCopyable(copyableStatus)
        }
    }

    @Test
    fun `sales of different contacts are refused for consolidation, sales of one contact are accepted`() {
        val contactId = UUID.randomUUID()

        saleSessionValidator.guardSingleContactAcrossSales(
            listOf(saleHeader(contactId), saleHeader(contactId))
        )
        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.guardSingleContactAcrossSales(
                listOf(saleHeader(contactId), saleHeader(UUID.randomUUID()))
            )
        }
    }

    @Test
    fun `a consolidation needs between two and twenty distinct sales`() {
        val contactId = UUID.randomUUID()

        saleSessionValidator.guardConsolidationSelectionSize(List(2) { saleHeader(contactId) })
        saleSessionValidator.guardConsolidationSelectionSize(List(20) { saleHeader(contactId) })
        listOf(0, 1, 21).forEach { refusedSize ->
            assertThrows(RtsGenericException::class.java) {
                saleSessionValidator.guardConsolidationSelectionSize(List(refusedSize) { saleHeader(contactId) })
            }
        }
        val selectedTwice = saleHeader(contactId)
        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.guardConsolidationSelectionSize(listOf(selectedTwice, selectedTwice))
        }
    }

    @Test
    fun `a sale refuses two lines of one product, a pending consolidation holds them`() {
        val firstLine = saleSessionLine(BigDecimal("10.00"))
        val secondLine = firstLine.copy(identity = SessionIdentity.mintFreshIdentity())

        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.validate(saleSession(listOf(firstLine, secondLine)))
        }
        saleSessionValidator.validate(pendingConsolidationSession(listOf(firstLine, secondLine)))
    }

    @Test
    fun `a pending consolidation refuses an order-level adjustment but accepts a line-level one`() {
        val saleSessionLine = saleSessionLine(BigDecimal("10.00"))
        val orderLevelAdjustment = percentageAdjustment(AdjustmentDirection.DISCOUNT, BigDecimal("5"), null)
        val lineLevelAdjustment = percentageAdjustment(AdjustmentDirection.DISCOUNT, BigDecimal("5"), saleSessionLine)

        saleSessionValidator.validate(pendingConsolidationSession(listOf(saleSessionLine), listOf(lineLevelAdjustment)))
        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.validate(
                pendingConsolidationSession(listOf(saleSessionLine), listOf(orderLevelAdjustment))
            )
        }
    }

    @Test
    fun `a pending consolidation refuses what the rules table refuses, a sale accepts all of it`() {
        val pendingSession = pendingConsolidationSession(listOf(saleSessionLine(BigDecimal("10.00"))))
        val saleSession = saleSession(listOf(saleSessionLine(BigDecimal("10.00"))))

        listOf(
            SaleSessionAction.UPDATE_CONTACT, SaleSessionAction.SAVE_DRAFT, SaleSessionAction.CONFIRM,
            SaleSessionAction.ADD_PAYMENT, SaleSessionAction.ADD_ORDER_LEVEL_ADJUSTMENT,
        ).forEach { action ->
            saleSessionValidator.guardAllowed(saleSession, action)
            assertThrows(RtsGenericException::class.java) { saleSessionValidator.guardAllowed(pendingSession, action) }
        }
        assertThrows(RtsGenericException::class.java) { saleSessionValidator.canAddPayments(pendingSession) }
    }

    @Test
    fun `a refusal says whether the table or the sale status caused it`() {
        val pendingSession = pendingConsolidationSession(emptyList())
        val confirmedSession = saleSession(emptyList()).copy(originalStatus = SaleStatus.CONFIRMED)

        val tableRefusal = assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.guardAllowed(pendingSession, SaleSessionAction.UPDATE_CONTACT)
        }
        val statusRefusal = assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.guardAllowed(confirmedSession, SaleSessionAction.EDIT_LINES)
        }

        assertEquals("The contact is the contact of the sales being combined", tableRefusal.message)
        assertTrue(statusRefusal.message.contains("CONFIRMED"))
    }

    @Test
    fun `an action that needs a mutable sale is refused once the sale is no longer a draft`() {
        val confirmedSession = saleSession(emptyList()).copy(originalStatus = SaleStatus.CONFIRMED)

        assertThrows(RtsGenericException::class.java) {
            saleSessionValidator.guardAllowed(confirmedSession, SaleSessionAction.EDIT_LINES)
        }
        saleSessionValidator.guardAllowed(confirmedSession, SaleSessionAction.REMOVE_PAYMENT)
    }

    @Test
    fun `a session is a pending consolidation exactly when it carries pending data`() {
        val pendingSession = pendingConsolidationSession(emptyList())

        assertRefusedAtConstruction { pendingSession.copy(pendingConsolidation = null) }
        assertRefusedAtConstruction { pendingSession.copy(kind = SaleSessionKind.SALE) }
    }

    @Test
    fun `a pending consolidation has no sale id, sale version or payments`() {
        val pendingSession = pendingConsolidationSession(emptyList())
        val payment = SaleSessionPayment(
            SessionIdentity.mintFreshIdentity(), UUID.randomUUID(), BigDecimal.ONE, null, null
        )

        assertRefusedAtConstruction { pendingSession.copy(saleId = UUID.randomUUID()) }
        assertRefusedAtConstruction { pendingSession.copy(saleVersion = 1L) }
        assertRefusedAtConstruction { pendingSession.copy(salePayments = listOf(payment)) }
    }

    private fun assertRefusedAtConstruction(buildSaleSession: () -> SaleSession) {
        assertThrows(RtsGenericException::class.java) { buildSaleSession() }
    }

    private fun pendingConsolidationSession(
        saleSessionLines: List<SaleSessionLine>,
        saleSessionAdjustments: List<SaleSessionAdjustment> = emptyList(),
    ) = saleSession(saleSessionLines, saleSessionAdjustments).copy(
        kind = SaleSessionKind.PENDING_CONSOLIDATION,
        pendingConsolidation = PendingConsolidation(emptyList(), emptyMap()),
    )

    private fun saleHeader(contactId: UUID) = SaleHeaderDto(
        id = UUID.randomUUID(),
        referenceNumber = "SALE01",
        version = 0,
        status = SaleStatus.DRAFT,
        contactId = contactId,
        soldById = null,
        dateSold = null,
        notes = null,
    )

    private fun percentageAdjustment(direction: AdjustmentDirection, percentage: BigDecimal, line: SaleSessionLine?) = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = line?.identity,
        adjustmentReasonId = UUID.randomUUID(),
        direction = direction,
        calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
        value = percentage,
        note = null,
        approvedById = null
    )

    private fun saleSessionLine(lineTotal: BigDecimal, quantity: BigDecimal = BigDecimal.ONE) = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = UUID.randomUUID(),
        productLabel = "Rice",
        productPrintLabel = "Rice",
        userProvidedQuantity = quantity,
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = lineTotal,
        defaultSalePrice = lineTotal
    )

    private fun priceOverrideAdjustment(line: SaleSessionLine, unitPrice: BigDecimal) = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = line.identity,
        adjustmentReasonId = priceOverrideReasonId,
        direction = AdjustmentDirection.SURCHARGE,
        calculationMethod = PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE,
        value = unitPrice,
        note = null,
        approvedById = null
    )

    private fun saleSession(saleSessionLines: List<SaleSessionLine>, saleSessionAdjustments: List<SaleSessionAdjustment> = emptyList()) =
        SaleSession(
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
            header = SaleSessionHeader(referenceNumber = null, contactId = UUID.randomUUID(), soldById = null, dateSold = null, notes = null),
            saleLines = saleSessionLines,
            saleAdjustments = saleSessionAdjustments,
            salePayments = emptyList(),
            totals = SaleSessionTotals.ZERO
        )
}
