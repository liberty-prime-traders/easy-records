package app.myliberty.easy_records.organizations.business.core.account.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SystemAccountTest {

    @Test
    fun `every system account has a name and a name that is unique among its siblings`() {
        assertTrue(SystemAccount.entries.all { it.accountName.isNotBlank() })

        val siblingNamesByParent = SystemAccount.entries.groupBy { it.parent }
        siblingNamesByParent.values.forEach { siblings ->
            assertEquals(siblings.size, siblings.map { it.accountName }.toSet().size)
        }
    }

    @Test
    fun `a system account is named as the chart of accounts shows it`() {
        assertEquals("Cash", SystemAccount.CASH.accountName)
        assertEquals("Current Assets", SystemAccount.CURRENT_ASSETS.accountName)
    }
}
