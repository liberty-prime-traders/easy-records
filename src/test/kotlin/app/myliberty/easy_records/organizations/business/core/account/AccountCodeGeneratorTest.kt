package app.myliberty.easy_records.organizations.business.core.account

import app.myliberty.easy_records.organizations.business.core.account.api.AccountType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class AccountCodeGeneratorTest {

    @Test
    fun `the first child under a parent with no siblings takes segment 001`() {
        assertEquals("100.001", AccountCodeGenerator.generateChildCode("100", emptyList()))
    }

    @Test
    fun `the next child takes the segment after the highest sibling`() {
        val siblings = listOf(accountDto("100.001"), accountDto("100.002"))

        assertEquals("100.003", AccountCodeGenerator.generateChildCode("100", siblings))
    }

    @Test
    fun `a gap among siblings is not filled, the next child takes the segment after the highest`() {
        val siblings = listOf(accountDto("100.001"), accountDto("100.003"))

        assertEquals("100.004", AccountCodeGenerator.generateChildCode("100", siblings))
    }

    @Test
    fun `the highest sibling wins even when siblings are listed out of order`() {
        val siblings = listOf(accountDto("100.003"), accountDto("100.001"))

        assertEquals("100.004", AccountCodeGenerator.generateChildCode("100", siblings))
    }

    @Test
    fun `the 999th child is allowed`() {
        val siblings = listOf(accountDto("100.998"))

        assertEquals("100.999", AccountCodeGenerator.generateChildCode("100", siblings))
    }

    @Test
    fun `a child beyond 999 under one parent is refused`() {
        val siblings = listOf(accountDto("100.999"))

        val thrown = assertThrows(IllegalStateException::class.java) {
            AccountCodeGenerator.generateChildCode("100", siblings)
        }
        assertEquals("Cannot generate more than 999 child accounts under the one parent.", thrown.message)
    }

    @Test
    fun `a malformed sibling code is refused rather than skipped`() {
        val siblings = listOf(accountDto("100.abc"))

        val thrown = assertThrows(IllegalStateException::class.java) {
            AccountCodeGenerator.generateChildCode("100", siblings)
        }
        assertEquals("An existing account with a malformed code format was found", thrown.message)
    }

    @Test
    fun `the first organization root takes code 100`() {
        assertEquals("100", AccountCodeGenerator.generateRootCode(emptyList()))
    }

    @Test
    fun `the next root takes the code after the highest organization root`() {
        val accounts = listOf(accountDto("100"), accountDto("101"))

        assertEquals("102", AccountCodeGenerator.generateRootCode(accounts))
    }

    @Test
    fun `a gap among organization roots is not filled`() {
        val accounts = listOf(accountDto("100"), accountDto("102"))

        assertEquals("103", AccountCodeGenerator.generateRootCode(accounts))
    }

    @Test
    fun `system roots below 100 do not move the organization root sequence`() {
        val accounts = listOf(accountDto("001"), accountDto("002"), accountDto("003"), accountDto("004"), accountDto("005"))

        assertEquals("100", AccountCodeGenerator.generateRootCode(accounts))
    }

    @Test
    fun `child accounts do not count toward the root sequence`() {
        val accounts = listOf(accountDto("100"), accountDto("100.001", parentAccountCode = "100"), accountDto("100.002", parentAccountCode = "100"))

        assertEquals("101", AccountCodeGenerator.generateRootCode(accounts))
    }

    private fun accountDto(code: String, parentAccountCode: String? = null) = AccountDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        code = code,
        name = "Account $code",
        accountType = AccountType.ASSET,
        currencyCode = "KES",
        accountIsActive = true,
        accountIsSystemMaintained = false,
        parentAccountCode = parentAccountCode
    )
}
