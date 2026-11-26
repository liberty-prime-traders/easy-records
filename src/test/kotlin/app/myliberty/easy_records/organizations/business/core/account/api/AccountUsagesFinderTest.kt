package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AccountUsagesFinderTest {

    private fun provider(usageType: AccountUsageType, references: List<String>) = object : AccountUsageProvider {
        override val usageType = usageType
        override fun getReferences(accountCode: String): List<String> = references
    }

    @Test
    fun `only providers that reference the account are reported`() {
        val finder = AccountUsagesFinder(
            listOf(provider(AccountUsageType.PAYMENT_METHOD, listOf("Till")), provider(AccountUsageType.LOAN, emptyList()))
        )

        assertEquals(
            listOf(AccountUsageDto(AccountUsageType.PAYMENT_METHOD, listOf("Till"))),
            finder.findUsagesForAccountCode("001.001.001")
        )
    }

    @Test
    fun `an account with no references can be changed`() {
        val finder = AccountUsagesFinder(listOf(provider(AccountUsageType.LOAN, emptyList())))

        assertDoesNotThrow { finder.failOnUsagesForCode("100", "be deactivated") }
    }

    @Test
    fun `an account with references is refused and the refused action is named`() {
        val finder = AccountUsagesFinder(listOf(provider(AccountUsageType.LOAN, listOf("Loan 12"))))

        val thrown = assertThrows(LibertyGenericException::class.java) {
            finder.failOnUsagesForCode("100", "be deactivated")
        }
        assertEquals("Account is in use and cannot be deactivated", thrown.message)
    }
}
