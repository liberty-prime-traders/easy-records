package app.myliberty.easy_records.organizations.business.core.account

import app.myliberty.easy_records.organizations.business.core.account.api.AccountType
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class AccountAssertionsSiblingNameTest {

    @Test
    fun `a name already used by a sibling is refused, ignoring case and surrounding whitespace`() {
        val siblings = listOf(accountDto("100.001", "Bank Loans"))

        val thrown = assertThrows(LibertyGenericException::class.java) {
            AccountAssertions.assertSiblingNameAvailable("  bank loans ", siblings)
        }
        assertEquals("Sibling is already registered with the same name", thrown.message)
    }

    @Test
    fun `a name no sibling uses is accepted`() {
        val siblings = listOf(accountDto("100.001", "Bank Loans"))

        assertDoesNotThrow {
            AccountAssertions.assertSiblingNameAvailable("Vehicle Loans", siblings)
        }
    }

    @Test
    fun `with no siblings any name is accepted`() {
        assertDoesNotThrow {
            AccountAssertions.assertSiblingNameAvailable("Bank Loans", emptyList())
        }
    }

    private fun accountDto(code: String, name: String) = AccountDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        code = code,
        name = name,
        accountType = AccountType.LIABILITY,
        currencyCode = "KES",
        accountIsActive = true,
        accountIsSystemMaintained = false,
        parentAccountCode = "100"
    )
}
