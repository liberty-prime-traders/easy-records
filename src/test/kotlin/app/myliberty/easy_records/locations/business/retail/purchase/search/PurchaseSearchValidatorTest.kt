package app.myliberty.easy_records.locations.business.retail.purchase.search

import app.myliberty.easy_records.locations.business.retail.purchase.api.search.PurchaseSearchParameters
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class PurchaseSearchValidatorTest {

    private val windowStart = OffsetDateTime.parse("2026-01-01T00:00:00Z")
    private val windowEnd = OffsetDateTime.parse("2026-02-01T00:00:00Z")
    private val validWindow = PurchaseSearchParameters(recordedFrom = windowStart, recordedBefore = windowEnd)

    @Test
    fun `a search with a recorded window of a month is accepted`() {
        assertDoesNotThrow { PurchaseSearchValidator.guardValidParameters(validWindow) }
    }

    @Test
    fun `a search without a recorded window is refused`() {
        listOf(
            PurchaseSearchParameters(),
            PurchaseSearchParameters(recordedFrom = windowStart),
            PurchaseSearchParameters(recordedBefore = windowEnd)
        ).forEach { parameters ->
            val refusal = assertThrows(LibertyGenericException::class.java) {
                PurchaseSearchValidator.guardValidParameters(parameters)
            }

            assertEquals("recordedFrom and recordedBefore are required", refusal.message)
        }
    }

    @Test
    fun `a recorded window of more than a year is refused and exactly a year is accepted`() {
        val exactlyOneYear = validWindow.copy(recordedBefore = windowStart.plusYears(1))
        val overOneYear = validWindow.copy(recordedBefore = windowStart.plusYears(1).plusSeconds(1))

        assertDoesNotThrow { PurchaseSearchValidator.guardValidParameters(exactlyOneYear) }
        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidParameters(overOneYear)
        }

        assertEquals("recordedFrom and recordedBefore must be at most 1 year apart", refusal.message)
    }

    @Test
    fun `a recorded window that ends before it starts is refused`() {
        val reversed = validWindow.copy(recordedFrom = windowEnd, recordedBefore = windowStart)

        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidParameters(reversed)
        }

        assertEquals("recordedFrom must not be after recordedBefore", refusal.message)
    }

    @Test
    fun `a purchase date range that ends before it starts is refused`() {
        val reversed = validWindow.copy(purchaseDateFrom = windowEnd, purchaseDateBefore = windowStart)

        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidParameters(reversed)
        }

        assertEquals("purchaseDateFrom must not be after purchaseDateBefore", refusal.message)
    }

    @Test
    fun `a minimum amount above the maximum is refused and equal bounds are accepted`() {
        val reversed = validWindow.copy(minAmount = BigDecimal("50"), maxAmount = BigDecimal("10"))
        val equalBounds = validWindow.copy(minAmount = BigDecimal("50"), maxAmount = BigDecimal("50"))

        assertDoesNotThrow { PurchaseSearchValidator.guardValidParameters(equalBounds) }
        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidParameters(reversed)
        }

        assertEquals("minAmount must not be greater than maxAmount", refusal.message)
    }

    @Test
    fun `a negative minimum or maximum amount is refused`() {
        val negativeMinimum = validWindow.copy(minAmount = BigDecimal("-0.01"))
        val negativeMaximum = validWindow.copy(maxAmount = BigDecimal("-0.01"))

        val minimumRefusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidParameters(negativeMinimum)
        }
        val maximumRefusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidParameters(negativeMaximum)
        }

        assertEquals("minAmount must not be negative", minimumRefusal.message)
        assertEquals("maxAmount must not be negative", maximumRefusal.message)
    }

    @Test
    fun `more than 20 purchase references or 100 suppliers are refused and the limits themselves are accepted`() {
        val atReferenceLimit = validWindow.copy(purchaseReferenceNumbers = List(20) { "PUR$it" })
        val overReferenceLimit = validWindow.copy(purchaseReferenceNumbers = List(21) { "PUR$it" })
        val atSupplierLimit = validWindow.copy(supplierIds = List(100) { UUID.randomUUID() })
        val overSupplierLimit = validWindow.copy(supplierIds = List(101) { UUID.randomUUID() })

        assertDoesNotThrow { PurchaseSearchValidator.guardValidParameters(atReferenceLimit) }
        assertDoesNotThrow { PurchaseSearchValidator.guardValidParameters(atSupplierLimit) }
        val referenceRefusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidParameters(overReferenceLimit)
        }
        val supplierRefusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidParameters(overSupplierLimit)
        }

        assertEquals("A maximum of 20 purchase reference numbers may be supplied", referenceRefusal.message)
        assertEquals("A maximum of 100 supplier ids may be supplied", supplierRefusal.message)
    }

    @Test
    fun `a purchase date range that is in order or a single instant is accepted`() {
        val inOrder = validWindow.copy(purchaseDateFrom = windowStart, purchaseDateBefore = windowEnd)
        val singleInstant = validWindow.copy(purchaseDateFrom = windowStart, purchaseDateBefore = windowStart)

        assertDoesNotThrow { PurchaseSearchValidator.guardValidParameters(inOrder) }
        assertDoesNotThrow { PurchaseSearchValidator.guardValidParameters(singleInstant) }
    }

    @Test
    fun `a payable range in order or a single instant is accepted and a reversed one is refused`() {
        assertDoesNotThrow { PurchaseSearchValidator.guardValidPayableRange(windowStart, windowEnd) }
        assertDoesNotThrow { PurchaseSearchValidator.guardValidPayableRange(windowStart, windowStart) }

        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseSearchValidator.guardValidPayableRange(windowEnd, windowStart)
        }

        assertEquals("orderedFrom must not be after orderedBefore", refusal.message)
    }

    @Test
    fun `a page size outside 1 to 200 is refused and the limits are accepted`() {
        listOf(1, 200).forEach { pageSize ->
            assertDoesNotThrow { PurchaseSearchValidator.guardValidPageSize(pageSize) }
        }

        listOf(0, 201).forEach { pageSize ->
            val refusal = assertThrows(LibertyGenericException::class.java) {
                PurchaseSearchValidator.guardValidPageSize(pageSize)
            }

            assertEquals("requestedSize must be between 1 and 200", refusal.message)
        }
    }
}
