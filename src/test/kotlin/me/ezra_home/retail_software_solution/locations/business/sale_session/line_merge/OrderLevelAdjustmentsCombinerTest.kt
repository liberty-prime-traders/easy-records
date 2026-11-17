package me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.math.BigDecimal
import java.util.UUID

class OrderLevelAdjustmentsCombinerTest {

    private val loyaltyReasonId = UUID.randomUUID()
    private val damageReasonId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()

    private fun combine(
        vararg adjustmentsWithSourceAmount: Pair<SaleSessionAdjustment, String>,
        combinedSubtotal: String,
    ): List<SaleSessionAdjustment> = OrderLevelAdjustmentsCombiner.combine(
        orderLevelSaleAdjustments = adjustmentsWithSourceAmount.map { it.first },
        sourceAmountByAdjustmentKey = adjustmentsWithSourceAmount.associate {
            it.first.identity.key() to BigDecimal(it.second)
        },
        combinedSubtotal = BigDecimal(combinedSubtotal),
    )

    @Test
    fun `equal percentages on equal sales stay the same percentage, they are not added`() {
        val combined = combine(
            orderLevelAdjustment(loyaltyReasonId, "5", note = "first") to "3.75",
            orderLevelAdjustment(loyaltyReasonId, "5", note = "second") to "3.75",
            combinedSubtotal = "150.00",
        )

        assertEquals(1, combined.size)
        assertEquals(0, BigDecimal("5").compareTo(combined.single().value))
        assertEquals("first; second", combined.single().note)
    }

    @Test
    fun `a percentage on one sale becomes a smaller percentage of the combined goods`() {
        val combined = combine(
            orderLevelAdjustment(loyaltyReasonId, "5") to "5.00",
            combinedSubtotal = "400.00",
        )

        assertEquals(0, BigDecimal("1.25").compareTo(combined.single().value))
    }

    @Test
    fun `different percentages on different sales give the percentage of the money they add up to`() {
        val combined = combine(
            orderLevelAdjustment(loyaltyReasonId, "10") to "10.00",
            orderLevelAdjustment(loyaltyReasonId, "5") to "10.00",
            combinedSubtotal = "200.00",
        )

        assertEquals(0, BigDecimal("10").compareTo(combined.single().value))
    }

    @Test
    fun `a percentage that cannot be reproduced exactly is refused`() {
        assertThrows(RtsGenericException::class.java) {
            combine(
                orderLevelAdjustment(loyaltyReasonId, "10") to "10.00",
                orderLevelAdjustment(loyaltyReasonId, "5") to "10.00",
                combinedSubtotal = "300.00",
            )
        }
        assertThrows(RtsGenericException::class.java) {
            combine(orderLevelAdjustment(loyaltyReasonId, "1") to "1.00", combinedSubtotal = "3.00")
        }
    }

    @Test
    fun `adjustments of different reasons stay apart and keep their first-seen order`() {
        val combined = combine(
            orderLevelAdjustment(damageReasonId, "1") to "1.00",
            orderLevelAdjustment(loyaltyReasonId, "2") to "2.00",
            orderLevelAdjustment(damageReasonId, "3") to "3.00",
            combinedSubtotal = "100.00",
        )

        assertEquals(listOf(damageReasonId, loyaltyReasonId), combined.map { it.adjustmentReasonId })
        assertEquals(0, BigDecimal("4").compareTo(combined.first().value))
    }

    @Test
    fun `adjustments with different approvers are never added together`() {
        val combined = combine(
            orderLevelAdjustment(loyaltyReasonId, "5", approvedById = managerId) to "5.00",
            orderLevelAdjustment(loyaltyReasonId, "5", approvedById = null) to "5.00",
            combinedSubtotal = "100.00",
        )

        assertEquals(2, combined.size)
    }

    @Test
    fun `a blank note stays null and a repeated note is kept once`() {
        val combined = combine(
            orderLevelAdjustment(loyaltyReasonId, "1", note = "  ") to "1.00",
            orderLevelAdjustment(loyaltyReasonId, "1", note = null) to "1.00",
            combinedSubtotal = "200.00",
        )
        assertNull(combined.single().note)

        val withRepeatedNote = combine(
            orderLevelAdjustment(loyaltyReasonId, "1", note = "same") to "1.00",
            orderLevelAdjustment(loyaltyReasonId, "1", note = "same") to "1.00",
            combinedSubtotal = "200.00",
        )
        assertEquals("same", withRepeatedNote.single().note)
    }

    @Test
    fun `nothing is carried when the combined goods total is zero`() {
        val combined = combine(orderLevelAdjustment(loyaltyReasonId, "5") to "5.00", combinedSubtotal = "0.00")

        assertEquals(emptyList<SaleSessionAdjustment>(), combined)
    }

    private fun orderLevelAdjustment(
        adjustmentReasonId: UUID,
        value: String,
        note: String? = null,
        approvedById: UUID? = null,
    ) = SaleSessionAdjustment(
        identity = SessionIdentity.mintFreshIdentity(),
        relatedSaleLineIdentity = null,
        adjustmentReasonId = adjustmentReasonId,
        direction = AdjustmentDirection.DISCOUNT,
        calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
        value = BigDecimal(value),
        note = note,
        approvedById = approvedById,
    )
}
