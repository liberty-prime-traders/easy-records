package app.myliberty.easy_records.util.business

import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import app.myliberty.easy_records.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TypedDecimalTest {

    private val one = BigDecimal.ONE

    @Test
    fun `values of different typed decimal types are never equal, even with the same number`() {
        assertNotEquals(Money.of(one) as Any, PositiveMoney.of(one) as Any)
        assertNotEquals(UserProvidedQuantity.of(one) as Any, PositiveUserProvidedQuantity.of(one) as Any)
        assertNotEquals(Money.of(one) as Any, UserProvidedQuantity.of(one) as Any)
    }

    @Test
    fun `the same type with the same number is equal and compares by value`() {
        assertEquals(PositiveMoney.of(BigDecimal("1.5")), PositiveMoney.of(BigDecimal("1.50")))
        assertEquals(-1, PositiveMoney.of(one).compareTo(PositiveMoney.of(BigDecimal("2"))))
    }
}
