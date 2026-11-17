package me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class LineWithAdjustmentsTest {

    private val saleLine = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = UUID.randomUUID(),
        productLabel = "Product",
        productPrintLabel = "Product",
        userProvidedQuantity = BigDecimal("1"),
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1,
        lineTotal = BigDecimal("10.00"),
        defaultSalePrice = BigDecimal("10.00"),
    )

    @Test
    fun `the price override is split from the other adjustments and all adjustments list the override first`() {
        val percentageDiscount = adjustment(PriceAdjustmentMethod.PERCENTAGE)
        val priceOverride = adjustment(PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE)

        val lineWithAdjustments = LineWithAdjustments.of(saleLine, listOf(percentageDiscount, priceOverride))

        assertEquals(priceOverride, lineWithAdjustments.priceOverride)
        assertEquals(listOf(percentageDiscount), lineWithAdjustments.otherAdjustments)
        assertEquals(listOf(priceOverride, percentageDiscount), lineWithAdjustments.allAdjustments)
    }

    @Test
    fun `a line without a price override has none`() {
        val lineWithAdjustments = LineWithAdjustments.of(saleLine, listOf(adjustment(PriceAdjustmentMethod.PERCENTAGE)))

        assertNull(lineWithAdjustments.priceOverride)
    }

    @Test
    fun `a line carrying two price overrides is refused`() {
        val twoOverrides = listOf(
            adjustment(PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE),
            adjustment(PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE),
        )

        assertThrows(IllegalStateException::class.java) { LineWithAdjustments.of(saleLine, twoOverrides) }
    }

    private fun adjustment(calculationMethod: PriceAdjustmentMethod) = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = saleLine.identity,
        adjustmentReasonId = UUID.randomUUID(),
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = calculationMethod,
        value = BigDecimal("10"),
        note = null,
        approvedById = null,
    )
}
