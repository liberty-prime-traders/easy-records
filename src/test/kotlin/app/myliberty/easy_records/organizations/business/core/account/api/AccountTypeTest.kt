package app.myliberty.easy_records.organizations.business.core.account.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccountTypeTest {

    @Test
    fun `each contra type is a contra of its base type only`() {
        assertTrue(AccountType.ASSET_CONTRA.isContraOf(AccountType.ASSET))
        assertTrue(AccountType.LIABILITY_CONTRA.isContraOf(AccountType.LIABILITY))
        assertTrue(AccountType.EQUITY_CONTRA.isContraOf(AccountType.EQUITY))
        assertTrue(AccountType.EXPENSE_CONTRA.isContraOf(AccountType.EXPENSE))
        assertTrue(AccountType.REVENUE_CONTRA.isContraOf(AccountType.REVENUE))
        assertFalse(AccountType.REVENUE_CONTRA.isContraOf(AccountType.EXPENSE))
        assertFalse(AccountType.ASSET_CONTRA.isContraOf(AccountType.LIABILITY))
        assertFalse(AccountType.LIABILITY_CONTRA.isContraOf(AccountType.ASSET))
        assertFalse(AccountType.EQUITY_CONTRA.isContraOf(AccountType.LIABILITY))
        assertFalse(AccountType.EXPENSE_CONTRA.isContraOf(AccountType.REVENUE))
        assertFalse(AccountType.ASSET.isContraOf(AccountType.ASSET))
    }

    @Test
    fun `every type maps to its category, contras included`() {
        assertEquals(AccountCategory.LIABILITY, AccountType.LIABILITY_CONTRA.category())
        assertEquals(AccountCategory.EQUITY, AccountType.EQUITY_CONTRA.category())
        assertEquals(AccountCategory.REVENUE, AccountType.REVENUE.category())
        assertEquals(AccountCategory.REVENUE, AccountType.REVENUE_CONTRA.category())
        assertEquals(AccountCategory.EXPENSE, AccountType.EXPENSE.category())
        assertEquals(AccountCategory.EXPENSE, AccountType.EXPENSE_CONTRA.category())
    }

    @Test
    fun `a credit on revenue adds to profit and a debit on revenue removes it`() {
        assertEquals(0, BigDecimal("10.00").compareTo(AccountType.REVENUE.profitChangeOf(EntryType.CREDIT, BigDecimal("10.00"))))
        assertEquals(0, BigDecimal("-10.00").compareTo(AccountType.REVENUE.profitChangeOf(EntryType.DEBIT, BigDecimal("10.00"))))
    }

    @Test
    fun `a debit on a revenue contra reduces profit`() {
        assertEquals(0, BigDecimal("-10.00").compareTo(AccountType.REVENUE_CONTRA.profitChangeOf(EntryType.DEBIT, BigDecimal("10.00"))))
    }

    @Test
    fun `a debit on expense reduces profit`() {
        assertEquals(0, BigDecimal("-10.00").compareTo(AccountType.EXPENSE.profitChangeOf(EntryType.DEBIT, BigDecimal("10.00"))))
    }

    @Test
    fun `entries on balance sheet accounts do not change profit`() {
        assertEquals(0, BigDecimal.ZERO.compareTo(AccountType.ASSET.profitChangeOf(EntryType.DEBIT, BigDecimal("10.00"))))
    }
}
