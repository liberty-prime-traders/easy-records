package app.myliberty.easy_records.locations.business.retail.sale_adjustment.api

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleAdjustmentSaveRequest
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSaveRequest
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.SaleAdjustmentEntity
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.SaleAdjustmentRepository
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.util.UUID

class SaleAdjustmentSyncerTest {

    private val savedSaleAdjustments = mutableListOf<SaleAdjustmentEntity>()
    private val saleAdjustmentRepository = mock(SaleAdjustmentRepository::class.java) { invocation ->
        if (invocation.method.name == "saveAll") {
            val saleAdjustmentEntities = invocation.getArgument<Iterable<SaleAdjustmentEntity>>(0).toList()
            saleAdjustmentEntities.forEach { if (it.id == null) it.id = UUID.randomUUID() }
            savedSaleAdjustments.addAll(saleAdjustmentEntities)
            saleAdjustmentEntities
        } else {
            null
        }
    }
    private val saleAdjustmentSyncer = SaleAdjustmentSyncer(saleAdjustmentRepository)

    private val saleId = UUID.randomUUID()
    private val locationProductId = UUID.randomUUID()
    private val firstLineClientKey = UUID.randomUUID()
    private val secondLineClientKey = UUID.randomUUID()
    private val firstLineId = UUID.randomUUID()
    private val secondLineId = UUID.randomUUID()
    private val saleLineIdsByClientKey = mapOf(firstLineClientKey to firstLineId, secondLineClientKey to secondLineId)
    private val persistedSaleLines = listOf(
        PersistedSaleLine(firstLineId, locationProductId, BigDecimal("1"), BigDecimal("100.00")),
        PersistedSaleLine(secondLineId, locationProductId, BigDecimal("1"), BigDecimal("300.00")),
    )

    @Test
    fun `a line adjustment is linked to and calculated from its own line when two lines share a product`() {
        val adjustmentClientKey = UUID.randomUUID()
        val saleSaveRequest = saleSaveRequest(
            percentageDiscount(adjustmentClientKey, relatedLineClientKey = secondLineClientKey)
        )

        val saleAdjustmentIdsByClientKey = syncNew(saleSaveRequest)

        val savedSaleAdjustment = savedSaleAdjustments.single()
        assertEquals(secondLineId, savedSaleAdjustment.saleLineId)
        assertEquals(BigDecimal("30.00"), savedSaleAdjustment.calculatedAmount)
        assertEquals(savedSaleAdjustment.id, saleAdjustmentIdsByClientKey.getValue(adjustmentClientKey))
    }

    @Test
    fun `an order-level adjustment has no line and is calculated from the sum of all lines`() {
        val saleSaveRequest = saleSaveRequest(percentageDiscount(UUID.randomUUID(), relatedLineClientKey = null))

        syncNew(saleSaveRequest)

        val savedSaleAdjustment = savedSaleAdjustments.single()
        assertNull(savedSaleAdjustment.saleLineId)
        assertEquals(BigDecimal("40.00"), savedSaleAdjustment.calculatedAmount)
    }

    @Test
    fun `a kept adjustment is relinked and recalculated against its current line`() {
        val keptAdjustment = SaleAdjustmentEntity(
            saleId = saleId,
            saleLineId = firstLineId,
            direction = AdjustmentDirection.DISCOUNT,
            calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
            value = BigDecimal("10"),
            calculatedAmount = BigDecimal("10.00"),
            adjustmentReasonId = UUID.randomUUID(),
        ).also { it.id = UUID.randomUUID() }
        val adjustmentClientKey = UUID.randomUUID()
        val saleSaveRequest = saleSaveRequest(
            percentageDiscount(
                adjustmentClientKey, relatedLineClientKey = secondLineClientKey, existingId = keptAdjustment.id
            )
        )

        val saleAdjustmentIdsByClientKey = saleAdjustmentSyncer.sync(
            saleId, saleSaveRequest, persistedSaleLines, saleLineIdsByClientKey,
            RetainedSaleAdjustments(listOf(keptAdjustment)),
        )

        assertEquals(secondLineId, keptAdjustment.saleLineId)
        assertEquals(BigDecimal("30.00"), keptAdjustment.calculatedAmount)
        assertEquals(keptAdjustment.id, saleAdjustmentIdsByClientKey.getValue(adjustmentClientKey))
    }

    private fun syncNew(saleSaveRequest: SaleSaveRequest): Map<UUID, UUID> = saleAdjustmentSyncer.sync(
        saleId, saleSaveRequest, persistedSaleLines, saleLineIdsByClientKey, RetainedSaleAdjustments(emptyList()),
    )

    private fun percentageDiscount(
        clientKey: UUID,
        relatedLineClientKey: UUID?,
        existingId: UUID? = null,
    ) = SaleAdjustmentSaveRequest(
        clientKey = clientKey,
        existingId = existingId,
        relatedSaleLineClientKey = relatedLineClientKey,
        adjustmentReasonId = UUID.randomUUID(),
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
        value = BigDecimal("10"),
        note = null,
        approvedById = null,
    )

    private fun saleSaveRequest(saleAdjustmentSaveRequest: SaleAdjustmentSaveRequest) = SaleSaveRequest(
        saleId = saleId,
        expectedVersion = null,
        contactId = UUID.randomUUID(),
        soldById = null,
        dateSold = null,
        notes = null,
        saleLines = emptyList(),
        saleAdjustments = listOf(saleAdjustmentSaveRequest),
        salePayments = emptyList(),
    )
}
