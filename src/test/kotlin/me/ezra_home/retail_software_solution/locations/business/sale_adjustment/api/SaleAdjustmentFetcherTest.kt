package me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentEntity
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.SaleAdjustmentRepository
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SaleAdjustmentFetcherTest {

    private val saleAdjustmentRepository = mock(SaleAdjustmentRepository::class.java)
    private val saleAdjustmentFetcher = SaleAdjustmentFetcher(saleAdjustmentRepository)

    @Test
    fun `adjustments are grouped by sale in query order and a sale without adjustments gets an empty list`() {
        val firstSaleId = UUID.randomUUID()
        val secondSaleId = UUID.randomUUID()
        val saleIdWithoutAdjustments = UUID.randomUUID()
        val firstSaleFirstAdjustment = adjustmentEntity(firstSaleId)
        val secondSaleAdjustment = adjustmentEntity(secondSaleId)
        val firstSaleSecondAdjustment = adjustmentEntity(firstSaleId)
        val saleIds = listOf(firstSaleId, secondSaleId, saleIdWithoutAdjustments)
        `when`(saleAdjustmentRepository.findBySaleIdInOrderByCreatedOnAscIdAsc(saleIds))
            .thenReturn(listOf(firstSaleFirstAdjustment, secondSaleAdjustment, firstSaleSecondAdjustment))

        val adjustmentsBySaleId = saleAdjustmentFetcher.getAdjustmentsBySaleId(saleIds)

        assertEquals(saleIds, adjustmentsBySaleId.keys.toList())
        assertEquals(
            listOf(firstSaleFirstAdjustment.id, firstSaleSecondAdjustment.id),
            adjustmentsBySaleId.getValue(firstSaleId).map { it.id },
        )
        assertEquals(listOf(secondSaleAdjustment.id), adjustmentsBySaleId.getValue(secondSaleId).map { it.id })
        assertEquals(emptyList<SaleAdjustmentDto>(), adjustmentsBySaleId.getValue(saleIdWithoutAdjustments))
    }

    @Test
    fun `the single-sale fetch is the one-id case of the batch`() {
        val saleId = UUID.randomUUID()
        val adjustment = adjustmentEntity(saleId)
        `when`(saleAdjustmentRepository.findBySaleIdInOrderByCreatedOnAscIdAsc(listOf(saleId)))
            .thenReturn(listOf(adjustment))

        val adjustments = saleAdjustmentFetcher.getAdjustments(saleId)

        assertEquals(listOf(adjustment.id), adjustments.map { it.id })
    }

    private fun adjustmentEntity(saleId: UUID) = SaleAdjustmentEntity(
        saleId = saleId,
        saleLineId = null,
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
        value = BigDecimal("10"),
        calculatedAmount = BigDecimal("1.00"),
        adjustmentReasonId = UUID.randomUUID(),
    ).also { it.id = UUID.randomUUID() }
}
