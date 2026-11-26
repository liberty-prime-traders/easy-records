package app.myliberty.easy_records.locations.business.retail.purchase

import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseStatus
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class PurchaseValidatorTest {

    private val milkProductId = UUID.randomUUID()
    private val breadProductId = UUID.randomUUID()

    @Test
    fun `two lines for the same product are refused`() {
        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseValidator.guardNoDuplicateProducts(listOf(lineFor(milkProductId), lineFor(milkProductId)))
        }

        assertEquals("A purchase cannot have more than one line for the same product.", refusal.message)
    }

    @Test
    fun `lines for different products are accepted`() {
        assertDoesNotThrow {
            PurchaseValidator.guardNoDuplicateProducts(listOf(lineFor(milkProductId), lineFor(breadProductId)))
        }
    }

    @Test
    fun `an order without lines is refused`() {
        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseValidator.guardHasLines(emptyList<Any>())
        }

        assertEquals("An order must have at least one item.", refusal.message)
    }

    @Test
    fun `an order with one line is accepted`() {
        assertDoesNotThrow { PurchaseValidator.guardHasLines(listOf(lineFor(milkProductId))) }
    }

    @Test
    fun `a zero unit cost is refused`() {
        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseValidator.guardPositiveUnitCosts(listOf(BigDecimal("5.00"), BigDecimal("0.00")))
        }

        assertEquals("Line unit cost must be positive.", refusal.message)
    }

    @Test
    fun `a negative unit cost is refused`() {
        val refusal = assertThrows(LibertyGenericException::class.java) {
            PurchaseValidator.guardPositiveUnitCosts(listOf(BigDecimal("-0.01")))
        }

        assertEquals("Line unit cost must be positive.", refusal.message)
    }

    @Test
    fun `the smallest money amount is an accepted unit cost`() {
        assertDoesNotThrow { PurchaseValidator.guardPositiveUnitCosts(listOf(BigDecimal("0.01"))) }
    }

    @Test
    fun `only a draft can be edited and every other status is refused with the same message`() {
        assertDoesNotThrow { PurchaseValidator.guardIsDraft(purchaseWithStatus(PurchaseStatus.DRAFT)) }

        PurchaseStatus.entries.filter { it != PurchaseStatus.DRAFT }.forEach { purchaseStatus ->
            val refusal = assertThrows(LibertyGenericException::class.java) {
                PurchaseValidator.guardIsDraft(purchaseWithStatus(purchaseStatus))
            }

            assertEquals(
                "Purchase is not in draft status anymore. The requested updates cannot be applied.",
                refusal.message,
                "status $purchaseStatus"
            )
        }
    }

    @Test
    fun `lines can be canceled on an ordered or partially delivered purchase only`() {
        val cancelableStatuses = listOf(PurchaseStatus.ORDERED, PurchaseStatus.PARTIALLY_DELIVERED)
        cancelableStatuses.forEach { purchaseStatus ->
            assertDoesNotThrow { PurchaseValidator.guardCanCancelLines(purchaseWithStatus(purchaseStatus)) }
        }

        val refusedStatuses = listOf(PurchaseStatus.DRAFT, PurchaseStatus.FULLY_DELIVERED, PurchaseStatus.CANCELED)
        refusedStatuses.forEach { purchaseStatus ->
            val refusal = assertThrows(LibertyGenericException::class.java) {
                PurchaseValidator.guardCanCancelLines(purchaseWithStatus(purchaseStatus))
            }

            assertEquals("Cannot cancel lines on a purchase with status $purchaseStatus", refusal.message)
        }
    }

    @Test
    fun `a delivery is accepted on an ordered or partially delivered purchase`() {
        assertDoesNotThrow { PurchaseValidator.guardCanReceiveDelivery(purchaseWithStatus(PurchaseStatus.ORDERED)) }
        assertDoesNotThrow {
            PurchaseValidator.guardCanReceiveDelivery(purchaseWithStatus(PurchaseStatus.PARTIALLY_DELIVERED))
        }
    }

    @Test
    fun `a delivery is refused with the reason for each status that cannot receive one`() {
        val expectedMessageByStatus = mapOf(
            PurchaseStatus.DRAFT to "Cannot record a delivery on a draft purchase. Confirm the order first.",
            PurchaseStatus.FULLY_DELIVERED to "Cannot record a delivery on a fully delivered purchase.",
            PurchaseStatus.CANCELED to "Cannot record a delivery on a canceled purchase."
        )

        expectedMessageByStatus.forEach { (purchaseStatus, expectedMessage) ->
            val refusal = assertThrows(LibertyGenericException::class.java) {
                PurchaseValidator.guardCanReceiveDelivery(purchaseWithStatus(purchaseStatus))
            }

            assertEquals(expectedMessage, refusal.message, "status $purchaseStatus")
        }
    }

    private fun lineFor(locationProductId: UUID) = object : HasLocationProduct {
        override val locationProductId: UUID = locationProductId
    }

    private fun purchaseWithStatus(purchaseStatus: PurchaseStatus) =
        PurchaseEntity(supplierId = UUID.randomUUID(), purchaseStatus = purchaseStatus)
}
