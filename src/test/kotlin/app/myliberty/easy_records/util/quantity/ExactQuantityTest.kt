package app.myliberty.easy_records.util.quantity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ExactQuantityTest {

    @Test
    fun `a whole quantity converts exactly to storage units`() {
        assertEquals(BigDecimal("18.0000"), ExactQuantity.ofStorageFromUserProvided(userProvided("1"), 18L).toStorageQuantity())
        assertEquals(BigDecimal("26.0000"), ExactQuantity.ofStorageFromUserProvided(userProvided("26"), 1L).toStorageQuantity())
    }

    @Test
    fun `a fractional root quantity and a fractional non-root quantity convert exactly`() {
        assertEquals(BigDecimal("0.5000"), ExactQuantity.ofStorageFromUserProvided(userProvided("0.5"), 1L).toStorageQuantity())
        assertEquals(BigDecimal("4.5000"), ExactQuantity.ofStorageFromUserProvided(userProvided("0.25"), 18L).toStorageQuantity())
    }

    @Test
    fun `selling 18000 single pieces consumes exactly 1000 bales`() {
        val onePiece = ExactQuantity.ofStorageFromUserProvided(userProvided("1"), 1L)
        val stock = ExactQuantity.ofStorageFromUserProvided(userProvided("1000"), 18L)

        val remaining = (1..18_000).fold(stock) { balance, _ -> balance - onePiece }

        assertTrue(remaining.isZero())
        assertEquals(BigDecimal("0.0000"), remaining.toStorageQuantity())
    }

    @Test
    fun `sufficiency compares exactly with no tolerance`() {
        val available = ExactQuantity.ofStorage(BigDecimal("17.0000"))

        assertTrue(available >= ExactQuantity.ofStorage(BigDecimal("17")))
        assertTrue(available < ExactQuantity.ofStorage(BigDecimal("17.0001")))
    }

    @Test
    fun `equal quantities of different scale are equal`() {
        assertEquals(ExactQuantity.ofStorage(BigDecimal("2.50")), ExactQuantity.ofStorage(BigDecimal("2.5000")))
        assertEquals(ExactQuantity.ofStorage(BigDecimal("2.50")).hashCode(), ExactQuantity.ofStorage(BigDecimal("2.5000")).hashCode())
    }

    @Test
    fun `reading storage digits that would be dropped throws instead of rounding`() {
        assertThrows(ArithmeticException::class.java) { ExactQuantity.ofStorage(BigDecimal("1.00001")).toStorageQuantity() }
    }

    @Test
    fun `display in a larger unit divides once at scale 4 with the chosen mode`() {
        val eighteenPieces = ExactQuantity.ofStorageFromUserProvided(userProvided("18"), 1L)
        val onePiece = ExactQuantity.ofStorageFromUserProvided(userProvided("1"), 1L)

        assertEquals(BigDecimal("1.0000"), eighteenPieces.toDisplayQuantity(18L, RoundingMode.DOWN))
        assertEquals(BigDecimal("0.0555"), onePiece.toDisplayQuantity(18L, RoundingMode.DOWN))
        assertEquals(BigDecimal("0.0556"), onePiece.toDisplayQuantity(18L, RoundingMode.HALF_UP))
    }

    @Test
    fun `a storage unit per unit below 1 is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ExactQuantity.ofStorageFromUserProvided(userProvided("1"), 0L) }
    }

    @Test
    fun `subtracting a quantity removes it exactly and can go below zero`() {
        val five = ExactQuantity.ofStorage(BigDecimal("5.5"))
        val two = ExactQuantity.ofStorage(BigDecimal("2.25"))

        assertEquals(BigDecimal("3.2500"), (five - two).toStorageQuantity())
        assertEquals(BigDecimal("-3.2500"), (two - five).toStorageQuantity())
    }

    @Test
    fun `adding does not change either operand`() {
        val first = ExactQuantity.ofStorage(BigDecimal("1"))
        val second = ExactQuantity.ofStorage(BigDecimal("2"))

        val sum = first + second

        assertEquals(BigDecimal("3.0000"), sum.toStorageQuantity())
        assertEquals(BigDecimal("1.0000"), first.toStorageQuantity())
        assertEquals(BigDecimal("2.0000"), second.toStorageQuantity())
    }

    @Test
    fun `only zero is zero`() {
        assertTrue(ExactQuantity.ZERO.isZero())
        assertTrue(ExactQuantity.ofStorage(BigDecimal("0.0000")).isZero())
        assertFalse(ExactQuantity.ofStorage(BigDecimal("0.0001")).isZero())
        assertFalse(ExactQuantity.ofStorage(BigDecimal("-0.0001")).isZero())
    }

    @Test
    fun `the zero constant reads as zero storage units`() {
        assertEquals(BigDecimal("0.0000"), ExactQuantity.ZERO.toStorageQuantity())
    }

    @Test
    fun `storage and display scale are four decimals`() {
        assertEquals(4, ExactQuantity.STORAGE_SCALE)
        assertEquals(4, ExactQuantity.DISPLAY_SCALE)
    }

    @Test
    fun `quantities order by value, negative below zero below positive`() {
        val negative = ExactQuantity.ofStorage(BigDecimal("-1"))
        val positive = ExactQuantity.ofStorage(BigDecimal("1"))

        assertTrue(negative.compareTo(ExactQuantity.ZERO) < 0)
        assertTrue(positive.compareTo(ExactQuantity.ZERO) > 0)
        assertTrue(ExactQuantity.ZERO.compareTo(negative) > 0)
        assertEquals(0, positive.compareTo(ExactQuantity.ofStorage(BigDecimal("1.0000"))))
    }

    @Test
    fun `quantities of different values are not equal and never equal another type`() {
        val one = ExactQuantity.ofStorage(BigDecimal("1"))

        assertNotEquals(one, ExactQuantity.ofStorage(BigDecimal("1.0001")))
        assertFalse(one.equals(BigDecimal("1")))
        assertFalse(one.equals(null))
    }

    @Test
    fun `zero quantities of different scale hash alike`() {
        assertEquals(ExactQuantity.ZERO.hashCode(), ExactQuantity.ofStorage(BigDecimal("0.0000")).hashCode())
        assertEquals(ExactQuantity.ofStorage(BigDecimal("100")).hashCode(), ExactQuantity.ofStorage(BigDecimal("1E+2")).hashCode())
    }

    @Test
    fun `a quantity prints its exact digits without rounding to storage scale`() {
        assertEquals("2.5", ExactQuantity.ofStorage(BigDecimal("2.5")).toString())
        assertEquals("0.00001", ExactQuantity.ofStorage(BigDecimal("0.00001")).toString())
    }

    @Test
    fun `a storage unit per unit below 1 is rejected with its message`() {
        val refusal = assertThrows(IllegalArgumentException::class.java) {
            ExactQuantity.ofStorageFromUserProvided(BigDecimal("1"), 0L)
        }
        assertEquals("storageUnitsPerUserProvidedUnit must be at least 1", refusal.message)
        assertThrows(IllegalArgumentException::class.java) { ExactQuantity.ofStorageFromUserProvided(BigDecimal("1"), -5L) }
    }

    @Test
    fun `a storage unit per user provided unit of exactly 1 is accepted and keeps the value`() {
        assertEquals(BigDecimal("2.5000"), ExactQuantity.ofStorageFromUserProvided(BigDecimal("2.5"), 1L).toStorageQuantity())
    }

    @Test
    fun `the user provided overload multiplies the typed value by the storage units per unit`() {
        val fromPositive = ExactQuantity.ofStorageFromUserProvided(userProvided("2.5"), 4L)
        val fromBigDecimal = ExactQuantity.ofStorageFromUserProvided(BigDecimal("2.5"), 4L)

        assertEquals(BigDecimal("10.0000"), fromPositive.toStorageQuantity())
        assertEquals(fromBigDecimal, fromPositive)
    }

    @Test
    fun `a display unit holding fewer than 1 storage unit is rejected with its message and 1 is accepted`() {
        val quantity = ExactQuantity.ofStorage(BigDecimal("2.5"))

        val refusal = assertThrows(IllegalArgumentException::class.java) { quantity.toDisplayQuantity(0L, RoundingMode.DOWN) }
        assertEquals("storageUnitsPerDisplayUnit must be at least 1", refusal.message)
        assertThrows(IllegalArgumentException::class.java) { quantity.toDisplayQuantity(-1L, RoundingMode.DOWN) }
        assertEquals(BigDecimal("2.5000"), quantity.toDisplayQuantity(1L, RoundingMode.DOWN))
    }

    @Test
    fun `display rounds the fifth decimal with the given mode in both directions`() {
        val quantity = ExactQuantity.ofStorage(BigDecimal("1"))

        assertEquals(BigDecimal("0.3333"), quantity.toDisplayQuantity(3L, RoundingMode.DOWN))
        assertEquals(BigDecimal("0.3334"), quantity.toDisplayQuantity(3L, RoundingMode.UP))
        assertEquals(BigDecimal("0.0001"), ExactQuantity.ofStorage(BigDecimal("1")).toDisplayQuantity(10_000L, RoundingMode.DOWN))
    }

    private fun userProvided(typedValue: String): PositiveUserProvidedQuantity = PositiveUserProvidedQuantity.of(BigDecimal(typedValue))
}
