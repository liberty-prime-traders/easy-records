package app.myliberty.easy_records.locations.business.retail.sale_session.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SaleSessionRulesTest {

    // Adding an action or a kind should fail here until someone decides what it means for each pairing.
    private val pendingConsolidationAnswers = mapOf(
        SaleSessionAction.UPDATE_CONTACT to false,
        SaleSessionAction.UPDATE_SOLD_BY to false,
        SaleSessionAction.UPDATE_DATE_SOLD to false,
        SaleSessionAction.UPDATE_NOTES to true,
        SaleSessionAction.ADD_LINE to false,
        SaleSessionAction.EDIT_LINES to true,
        SaleSessionAction.RESTORE_REMOVED_LINE to true,
        SaleSessionAction.ADD_LINE_ADJUSTMENT to true,
        SaleSessionAction.ADD_ORDER_LEVEL_ADJUSTMENT to false,
        SaleSessionAction.REMOVE_ADJUSTMENT to true,
        SaleSessionAction.SAVE_DRAFT to false,
        SaleSessionAction.CONFIRM to false,
        SaleSessionAction.GENERATE_CONSOLIDATED_SALE to true,
        SaleSessionAction.ADD_PAYMENT to false,
        SaleSessionAction.REMOVE_PAYMENT to false,
        SaleSessionAction.VOID_SALE to true,
    )

    @Test
    fun `every action is decided for the pending consolidation kind`() {
        assertEquals(SaleSessionAction.entries.toSet(), pendingConsolidationAnswers.keys)
        SaleSessionAction.entries.forEach { action ->
            assertEquals(
                pendingConsolidationAnswers.getValue(action),
                SaleSessionRules.allows(SaleSessionKind.PENDING_CONSOLIDATION, action),
                action.name
            )
        }
    }

    @Test
    fun `a sale allows every action except the two only a pending consolidation has`() {
        val pendingConsolidationOnlyActions = setOf(
            SaleSessionAction.RESTORE_REMOVED_LINE, SaleSessionAction.GENERATE_CONSOLIDATED_SALE
        )
        SaleSessionAction.entries.forEach { action ->
            assertEquals(
                action !in pendingConsolidationOnlyActions,
                SaleSessionRules.allows(SaleSessionKind.SALE, action),
                action.name
            )
        }
    }

    @Test
    fun `actions that need a mutable sale are exactly the ones that change the draft, apart from payments, voids and notes`() {
        val actionsWithTheirOwnStatusRule = setOf(
            SaleSessionAction.ADD_PAYMENT, SaleSessionAction.REMOVE_PAYMENT, SaleSessionAction.VOID_SALE,
            SaleSessionAction.UPDATE_NOTES
        )
        SaleSessionAction.entries.forEach { action ->
            assertEquals(action !in actionsWithTheirOwnStatusRule, action.needsMutableSale(), action.name)
        }
    }

    @Test
    fun `every refusal says why`() {
        SaleSessionAction.entries
            .forEach { action ->
                SaleSessionKind.entries.forEach { kind ->
                    val refusal = SaleSessionRules.refusalOrNull(kind, action)
                    if (refusal != null) assertTrue(refusal.isNotBlank(), "$kind $action")
                }
            }
    }

    @Test
    fun `only a sale reserves stock`() {
        assertTrue(SaleSessionRules.reservesStock(SaleSessionKind.SALE))
        assertFalse(SaleSessionRules.reservesStock(SaleSessionKind.PENDING_CONSOLIDATION))
    }

    @Test
    fun `only a pending consolidation keeps removed lines`() {
        assertFalse(SaleSessionRules.keepsRemovedLines(SaleSessionKind.SALE))
        assertTrue(SaleSessionRules.keepsRemovedLines(SaleSessionKind.PENDING_CONSOLIDATION))
    }

    @Test
    fun `only a pending consolidation holds one product on several lines`() {
        assertFalse(SaleSessionRules.allowsDuplicateProducts(SaleSessionKind.SALE))
        assertTrue(SaleSessionRules.allowsDuplicateProducts(SaleSessionKind.PENDING_CONSOLIDATION))
    }
}
