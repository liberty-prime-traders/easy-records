package app.myliberty.easy_records.locations.business.retail.purchase.api.payment

import app.myliberty.easy_records.locations.business.retail.delivery.api.PurchaseDeliveryDataFetcher
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseLineEntity
import app.myliberty.easy_records.locations.business.retail.purchase.PurchaseLineRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.util.UUID

class PurchasePaymentCeilingServiceTest {

    private val purchasePaymentCeilingService = PurchasePaymentCeilingService(
        mock(PurchaseLineRepository::class.java),
        mock(PurchaseDeliveryDataFetcher::class.java)
    )

    private fun line(ordered: String, unitCost: String, delivered: String = "0", canceled: String = "0") =
        PurchaseLineEntity(
            purchaseId = UUID.randomUUID(),
            locationProductId = UUID.randomUUID(),
            userProvidedQuantityOrdered = BigDecimal(ordered),
            unitCost = BigDecimal(unitCost),
            unitId = UUID.randomUUID(),
            storageUnitsPerUserProvidedUnit = 1L,
            storageQuantityDelivered = BigDecimal(delivered),
            storageQuantityCanceled = BigDecimal(canceled)
        )

    private fun ceilingOf(lines: List<PurchaseLineEntity>, deliveredTotal: String) =
        purchasePaymentCeilingService.computeCeiling(lines, BigDecimal(deliveredTotal))

    private fun assertAmount(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")

    @Test
    fun `nothing delivered the ceiling is what was ordered`() {
        val ceiling = ceilingOf(listOf(line("10", "5.00"), line("12", "2.50")), "0")

        assertAmount("80.00", ceiling.amount)
        assertFalse(ceiling.isFullyDelivered)
    }

    @Test
    fun `a canceled quantity is no longer part of what was ordered`() {
        val ceiling = ceilingOf(listOf(line("10", "5.00", canceled = "4"), line("12", "2.50")), "0")

        assertAmount("60.00", ceiling.amount)
        assertAmount("60.00", ceiling.orderedTotal)
    }

    @Test
    fun `every line canceled with nothing delivered leaves a ceiling of zero and is not fully delivered`() {
        val ceiling = ceilingOf(listOf(line("10", "5.00", canceled = "10"), line("12", "2.50", canceled = "12")), "0")

        assertAmount("0", ceiling.amount)
        assertFalse(ceiling.isFullyDelivered)
    }

    @Test
    fun `mid flight with less delivered than ordered the ceiling stays at the order`() {
        val ceiling = ceilingOf(listOf(line("10", "5.00", delivered = "4"), line("12", "2.50")), "20.00")

        assertAmount("80.00", ceiling.amount)
        assertFalse(ceiling.isFullyDelivered)
    }

    @Test
    fun `mid flight with the supplier charging more than ordered the ceiling is what was delivered`() {
        val ceiling = ceilingOf(listOf(line("10", "5.00", delivered = "4"), line("12", "2.50")), "95.00")

        assertAmount("95.00", ceiling.amount)
        assertFalse(ceiling.isFullyDelivered)
    }

    @Test
    fun `mid flight after canceling some of the rest the ceiling is the cancel adjusted order`() {
        val ceiling = ceilingOf(
            listOf(line("10", "5.00", delivered = "4", canceled = "6"), line("12", "2.50")),
            "20.00"
        )

        assertAmount("50.00", ceiling.amount)
        assertFalse(ceiling.isFullyDelivered)
    }

    @Test
    fun `once every line is delivered the ceiling is what was delivered even when that is less than ordered`() {
        val ceiling = ceilingOf(
            listOf(line("10", "5.00", delivered = "10"), line("12", "2.50", delivered = "12")),
            "70.00"
        )

        assertAmount("70.00", ceiling.amount)
        assertTrue(ceiling.isFullyDelivered)
    }

    @Test
    fun `delivered plus canceled covering every line counts as fully delivered`() {
        val ceiling = ceilingOf(
            listOf(line("10", "5.00", delivered = "4", canceled = "6"), line("12", "2.50", canceled = "12")),
            "20.00"
        )

        assertAmount("20.00", ceiling.amount)
        assertTrue(ceiling.isFullyDelivered)
    }
}
