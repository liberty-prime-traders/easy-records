package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.organizations.business.core.account.AccountBalance
import app.myliberty.easy_records.organizations.business.core.account.AccountCache
import app.myliberty.easy_records.organizations.business.core.account.AccountDto
import app.myliberty.easy_records.organizations.business.core.account.AccountRepository
import app.myliberty.easy_records.organizations.business.core.account.AccountResponseBuilder
import app.myliberty.easy_records.organizations.business.core.account.ChildAccountCreator
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class AccountServiceQueriesTest {

    private val accountCache = mock(AccountCache::class.java)
    private val accountRepository = mock(AccountRepository::class.java)
    private val accountStructureLock = mock(AccountStructureLock::class.java)
    private val accountService = AccountService(
        accountCache,
        accountRepository,
        mock(AccountResponseBuilder::class.java),
        mock(ChildAccountCreator::class.java),
        accountStructureLock,
        mock(AccountUsagesFinder::class.java)
    )

    @Test
    fun `the tree marks accounts without children as leaves and contra accounts as contra`() {
        `when`(accountCache.getAll()).thenReturn(
            listOf(
                userAccount("100", AccountType.LIABILITY, parentAccountCode = null),
                userAccount("100.001", AccountType.ASSET_CONTRA, parentAccountCode = "100")
            )
        )

        val nodesByCode = accountService.getAccountTree().associateBy { it.code }

        assertFalse(nodesByCode.getValue("100").isLeaf)
        assertFalse(nodesByCode.getValue("100").isContra)
        assertTrue(nodesByCode.getValue("100.001").isLeaf)
        assertTrue(nodesByCode.getValue("100.001").isContra)
    }

    @Test
    fun `account names are keyed by code and carry the display label`() {
        val liabilityAccount = userAccount("100", AccountType.LIABILITY, parentAccountCode = null)
        `when`(accountCache.getAll()).thenReturn(listOf(liabilityAccount))

        assertEquals(mapOf("100" to liabilityAccount.label), accountService.getAccountNamesByCode())
    }

    @Test
    fun `fresh balances are keyed by account code`() {
        `when`(accountRepository.findBalancesByCodes(setOf("100"))).thenReturn(
            listOf(
                object : AccountBalance {
                    override val code = "100"
                    override val currentBalance = BigDecimal("25.00")
                }
            )
        )

        assertEquals(0, BigDecimal("25.00").compareTo(accountService.getFreshBalancesByCodes(setOf("100")).getValue("100")))
    }

    @Test
    fun `setting an account to the state it already has writes nothing`() {
        val activeAccount = userAccount("100", AccountType.LIABILITY, parentAccountCode = null, accountIsActive = true)
        `when`(accountCache.getAll()).thenReturn(listOf(activeAccount))
        `when`(accountCache.getAllFresh()).thenReturn(listOf(activeAccount))

        accountService.toggleActive(activeAccount.id, true)

        assertTrue(mockingDetails(accountCache).invocations.none { invocation -> invocation.method.name == "update" })
        verify(accountStructureLock).acquire("100")
    }

    @Test
    fun `a system account cannot be activated or deactivated`() {
        val systemAccount = RecordingAccountService.accountDto(SystemAccount.CASH, accountIsActive = true)
        `when`(accountCache.getAll()).thenReturn(listOf(systemAccount))
        `when`(accountCache.getAllFresh()).thenReturn(listOf(systemAccount))

        val thrown = assertThrows(LibertyGenericException::class.java) {
            accountService.toggleActive(systemAccount.id, false)
        }

        assertEquals("System accounts cannot be activated or deactivated", thrown.message)
        assertTrue(mockingDetails(accountCache).invocations.none { invocation -> invocation.method.name == "update" })
    }

    @Test
    fun `an unknown account id is refused before any lock is taken`() {
        `when`(accountCache.getAll()).thenReturn(emptyList())

        val thrown = assertThrows(LibertyGenericException::class.java) {
            accountService.toggleActive(UUID.randomUUID(), false)
        }

        assertEquals("Account not found", thrown.message)
        verify(accountStructureLock, never()).acquire(org.mockito.ArgumentMatchers.anyString())
    }

    private fun userAccount(
        code: String,
        accountType: AccountType,
        parentAccountCode: String?,
        accountIsActive: Boolean = true
    ) = AccountDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        code = code,
        name = "Account $code",
        accountType = accountType,
        currencyCode = "KES",
        accountIsActive = accountIsActive,
        accountIsSystemMaintained = false,
        parentAccountCode = parentAccountCode
    )
}
