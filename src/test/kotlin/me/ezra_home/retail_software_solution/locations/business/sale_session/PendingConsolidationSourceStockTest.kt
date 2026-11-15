package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleLineDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.StockHeldBySales
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class PendingConsolidationSourceStockTest {

    private val sugarId = UUID.randomUUID()
    private val riceId = UUID.randomUUID()

    @Test
    fun `a draft source reserves stock and a confirmed source has taken it, in storage units`() {
        val draftSale = header(SaleStatus.DRAFT)
        val confirmedSale = header(SaleStatus.CONFIRMED)

        val stockHeld = PendingConsolidationSourceStock.calculate(
            listOf(draftSale, confirmedSale),
            mapOf(
                draftSale.id to listOf(line(sugarId, "2", 12), line(riceId, "1", 1)),
                confirmedSale.id to listOf(line(sugarId, "3", 1)),
            ),
        )

        assertEquals(0, BigDecimal("24").compareTo(stockHeld.getValue(sugarId).storageQuantityReserved))
        assertEquals(0, BigDecimal("3").compareTo(stockHeld.getValue(sugarId).storageQuantityTaken))
        assertEquals(0, BigDecimal("1").compareTo(stockHeld.getValue(riceId).storageQuantityReserved))
        assertEquals(0, BigDecimal.ZERO.compareTo(stockHeld.getValue(riceId).storageQuantityTaken))
    }

    @Test
    fun `lines of one product across sources add up`() {
        val firstDraft = header(SaleStatus.DRAFT)
        val secondDraft = header(SaleStatus.DRAFT)

        val stockHeld = PendingConsolidationSourceStock.calculate(
            listOf(firstDraft, secondDraft),
            mapOf(firstDraft.id to listOf(line(sugarId, "5", 1)), secondDraft.id to listOf(line(sugarId, "4", 1))),
        )

        assertEquals(0, BigDecimal("9").compareTo(stockHeld.getValue(sugarId).storageQuantityReserved))
    }

    @Test
    fun `a discarded source holds nothing`() {
        val discardedSale = header(SaleStatus.DISCARDED)

        val stockHeld = PendingConsolidationSourceStock.calculate(
            listOf(discardedSale), mapOf(discardedSale.id to listOf(line(sugarId, "5", 1)))
        )

        assertEquals(StockHeldBySales.NONE, stockHeld.getValue(sugarId))
        assertTrue(stockHeld.keys == setOf(sugarId))
    }

    private fun header(status: SaleStatus) = SaleHeaderDto(
        id = UUID.randomUUID(),
        referenceNumber = "SALE01",
        version = 0,
        status = status,
        contactId = UUID.randomUUID(),
        soldById = null,
        dateSold = null,
        notes = null,
    )

    private fun line(locationProductId: UUID, quantity: String, storageUnitsPerUnit: Long) = SaleLineDto(
        id = UUID.randomUUID(),
        locationProductId = locationProductId,
        productLabel = "Product",
        productPrintLabel = "Product",
        userProvidedQuantity = BigDecimal(quantity),
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = storageUnitsPerUnit,
        lineTotal = BigDecimal("10.00"),
        defaultSalePrice = BigDecimal("10.00"),
    )
}
