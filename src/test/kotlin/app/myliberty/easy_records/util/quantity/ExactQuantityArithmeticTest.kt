package app.myliberty.easy_records.util.quantity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ExactQuantityArithmeticTest {

    @Test
    fun `quantities add exactly across scales`() {
        val sum = ExactQuantity.ofStorage(BigDecimal("1.5")) + ExactQuantity.ofStorage(BigDecimal("2.25"))

        assertEquals(ExactQuantity.ofStorage(BigDecimal("3.75")), sum)
    }

    @Test
    fun `only a quantity above zero is positive`() {
        assertTrue(ExactQuantity.ofStorage(BigDecimal("0.0001")).isPositive())
        assertFalse(ExactQuantity.ZERO.isPositive())
        assertFalse(ExactQuantity.ofStorage(BigDecimal("-1")).isPositive())
    }

    @Test
    fun `a quantity prints as its plain decimal without an exponent`() {
        assertEquals("1000000", ExactQuantity.ofStorage(BigDecimal("1E+6")).toString())
    }
}
