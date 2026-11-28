package app.myliberty.easy_records.util.quantity

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class UserProvidedQuantityTest {

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `more than two significant decimals is rejected, never rounded`() {
        assertThrows(LibertyGenericException::class.java) { UserProvidedQuantity.of(BigDecimal("1.255")) }
    }

    @Test
    fun `trailing zeros beyond two decimals are accepted and keep the typed value`() {
        assertEquals(BigDecimal("1.2500"), UserProvidedQuantity.of(BigDecimal("1.2500")).toBigDecimal())
    }

    @Test
    fun `quantities that differ only in scale are equal and hash alike`() {
        val twoDecimals = UserProvidedQuantity.of(BigDecimal("1.50"))
        val fourDecimals = UserProvidedQuantity.of(BigDecimal("1.5000"))
        assertEquals(twoDecimals, fourDecimals)
        assertEquals(twoDecimals.hashCode(), fourDecimals.hashCode())
    }

    @Test
    fun `a negative quantity is rejected and zero is accepted`() {
        val exception = assertThrows(LibertyGenericException::class.java) { UserProvidedQuantity.of(BigDecimal("-0.5")) }
        assertEquals("Quantity must not be negative", exception.message)
        assertTrue(UserProvidedQuantity.of(BigDecimal("0.00")).isZero())
    }

    @Test
    fun `a magnitude above the maximum is rejected, including an enormous exponent, and the maximum itself is accepted`() {
        assertEquals(QuantityLimits.MAX_USER_QUANTITY, UserProvidedQuantity.of(QuantityLimits.MAX_USER_QUANTITY).toBigDecimal())
        listOf(QuantityLimits.MAX_USER_QUANTITY.add(BigDecimal("0.01")), BigDecimal("1e999999999")).forEach { outOfRange ->
            val exception = assertThrows(LibertyGenericException::class.java) { UserProvidedQuantity.of(outOfRange) }
            assertEquals("Quantity must not exceed 999999999999.99", exception.message)
        }
    }

    @Test
    fun `it travels as a bare JSON number in both directions`() {
        assertEquals("2.5", objectMapper.writeValueAsString(UserProvidedQuantity.of(BigDecimal("2.5"))))
        assertEquals(UserProvidedQuantity.of(BigDecimal("2.5")), objectMapper.readValue("2.5", UserProvidedQuantity::class.java))
    }

    @Test
    fun `a third significant decimal is refused with the decimal places message and two are accepted`() {
        val exception = assertThrows(LibertyGenericException::class.java) { UserProvidedQuantity.of(BigDecimal("1.255")) }

        assertEquals("Quantity can have at most 2 decimal places", exception.message)
        assertEquals(BigDecimal("1.25"), UserProvidedQuantity.of(BigDecimal("1.25")).toBigDecimal())
        assertEquals(BigDecimal("0.01"), UserProvidedQuantity.of(BigDecimal("0.01")).toBigDecimal())
    }

    @Test
    fun `a tiny nonzero quantity below two decimals is refused rather than treated as zero`() {
        assertThrows(LibertyGenericException::class.java) { UserProvidedQuantity.of(BigDecimal("0.001")) }
    }

    @Test
    fun `only zero reads as zero`() {
        assertTrue(UserProvidedQuantity.of(BigDecimal("0")).isZero())
        assertFalse(UserProvidedQuantity.of(BigDecimal("0.01")).isZero())
        assertFalse(UserProvidedQuantity.of(BigDecimal("5")).isZero())
    }

    @Test
    fun `a small negative quantity is rejected with the negative message`() {
        val exception = assertThrows(LibertyGenericException::class.java) { UserProvidedQuantity.of(BigDecimal("-0.01")) }

        assertEquals("Quantity must not be negative", exception.message)
    }

    @Test
    fun `a quantity just under the maximum is accepted and keeps its value`() {
        val justUnderMaximum = QuantityLimits.MAX_USER_QUANTITY.subtract(BigDecimal("0.01"))

        assertEquals(justUnderMaximum, UserProvidedQuantity.of(justUnderMaximum).toBigDecimal())
    }

    @Test
    fun `quantities of different values are not equal and never equal a bare number`() {
        val quantity = UserProvidedQuantity.of(BigDecimal("1.5"))

        assertNotEquals(quantity, UserProvidedQuantity.of(BigDecimal("1.51")))
        assertFalse(quantity.equals(BigDecimal("1.5")))
    }

    @Test
    fun `a JSON number outside the limits is refused on the way in`() {
        assertThrows(Exception::class.java) { objectMapper.readValue("-1", UserProvidedQuantity::class.java) }
        assertThrows(Exception::class.java) { objectMapper.readValue("1.234", UserProvidedQuantity::class.java) }
    }
}
