package app.myliberty.easy_records.util.quantity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class BaseUnitDisplayTest {

    @Test
    fun `the conversion factor is the line unit's size over the base unit's size`() {
        assertEquals(0, BigDecimal("1.5").compareTo(BaseUnitDisplay.conversionFactor(3, 2)))
    }

    @Test
    fun `a typed quantity is shown in the base unit rounded as the caller names`() {
        val roundedUp = BaseUnitDisplay.baseQuantity(BigDecimal("1"), 1, 3, RoundingMode.UP)
        val roundedDown = BaseUnitDisplay.baseQuantity(BigDecimal("1"), 1, 3, RoundingMode.DOWN)

        assertEquals(BigDecimal("0.3334"), roundedUp)
        assertEquals(BigDecimal("0.3333"), roundedDown)
    }

    @Test
    fun `a typed quantity of whole base units shows exactly`() {
        assertEquals(BigDecimal("2.5000"), BaseUnitDisplay.baseQuantity(BigDecimal("30"), 1, 12, RoundingMode.HALF_UP))
    }

    @Test
    fun `a storage quantity is shown in the base unit rounded as the caller names`() {
        assertEquals(
            BigDecimal("2.0000"),
            BaseUnitDisplay.storageQuantityInBaseUnit(BigDecimal("24"), 12, RoundingMode.HALF_UP)
        )
    }

    @Test
    fun `the price per base unit is the total scaled to the base unit's share of the stored quantity`() {
        assertEquals(0, BigDecimal("5").compareTo(BaseUnitDisplay.pricePerBaseUnit(BigDecimal("10"), BigDecimal("4"), 2)))
    }

    @Test
    fun `there is no price per base unit for a zero stored quantity`() {
        assertNull(BaseUnitDisplay.pricePerBaseUnit(BigDecimal("10"), BigDecimal.ZERO, 2))
    }
}
