package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.organizations.business.core.account.AccountCache
import app.myliberty.easy_records.organizations.business.core.account.AccountRepository
import app.myliberty.easy_records.organizations.business.core.account.AccountResponseBuilder
import app.myliberty.easy_records.organizations.business.core.account.ChildAccountCreator
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

// A fresh read taken before the lock would let a concurrent toggle or rename be overwritten by stale state.
class AccountServiceLockOrderTest {

    private val userAccountDto = RecordingAccountService.accountDto(SystemAccount.CASH, accountIsActive = true)
        .copy(accountIsSystemMaintained = false)
    private val accountCache = mock(AccountCache::class.java).also { accountCache ->
        `when`(accountCache.getAll()).thenReturn(listOf(userAccountDto))
        `when`(accountCache.getAllFresh()).thenReturn(listOf(userAccountDto))
    }
    private val accountStructureLock = mock(AccountStructureLock::class.java)
    private val accountUsagesFinder = mock(AccountUsagesFinder::class.java)
    private val accountService = AccountService(
        accountCache,
        mock(AccountRepository::class.java),
        mock(AccountResponseBuilder::class.java),
        mock(ChildAccountCreator::class.java),
        accountStructureLock,
        accountUsagesFinder
    )

    @Test
    fun `toggleActive takes the account lock before reading fresh state`() {
        accountService.toggleActive(userAccountDto.id, setActive = false)

        val lockThenFreshRead = inOrder(accountStructureLock, accountCache)
        lockThenFreshRead.verify(accountStructureLock).acquire(userAccountDto.code)
        lockThenFreshRead.verify(accountCache).getAllFresh()
    }

    @Test
    fun `deactivating an account that a payment method, tax type or expense type uses is refused`() {
        doThrow(RtsGenericException("Account is in use and cannot be deactivated"))
            .`when`(accountUsagesFinder).failOnUsagesForCode(userAccountDto.code, "be deactivated")

        assertThrows(RtsGenericException::class.java) {
            accountService.toggleActive(userAccountDto.id, setActive = false)
        }
    }

    @Test
    fun `reactivating an account does not consult its usages`() {
        val inactiveAccountDto = userAccountDto.copy(accountIsActive = false)
        `when`(accountCache.getAllFresh()).thenReturn(listOf(inactiveAccountDto))

        accountService.toggleActive(inactiveAccountDto.id, setActive = true)

        verify(accountUsagesFinder, never()).failOnUsagesForCode(inactiveAccountDto.code, "be deactivated")
    }
}
