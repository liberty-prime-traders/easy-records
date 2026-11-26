package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.organizations.business.core.account.AccountCache
import app.myliberty.easy_records.organizations.business.core.account.AccountRepository
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AccountDataFetcherTest {

    private val accountRepository = mock(AccountRepository::class.java)
    private val accountCache = mock(AccountCache::class.java)
    private val accountDataFetcher = AccountDataFetcher(accountRepository, accountCache)

    @Test
    fun `an unknown account code is refused`() {
        `when`(accountCache.getAll()).thenReturn(emptyList())

        val thrown = assertThrows(LibertyGenericException::class.java) {
            accountDataFetcher.getByCode("999")
        }
        assertEquals("Account 999 does not exist", thrown.message)
    }

    @Test
    fun `the lookup reports whether the account has children`() {
        val parent = RecordingAccountService.accountDto(SystemAccount.DIGITAL_PAYMENTS, accountIsActive = true)
        `when`(accountCache.getAll()).thenReturn(listOf(parent))
        `when`(accountRepository.existsByParentAccountCode(SystemAccount.DIGITAL_PAYMENTS.code)).thenReturn(true)

        val lookup = accountDataFetcher.getByCode(SystemAccount.DIGITAL_PAYMENTS.code)

        assertTrue(lookup.hasChildren)
        assertEquals(parent.accountType.normalBalance, lookup.normalBalanceEntryType)
    }

    @Test
    fun `the lookup reports an account without children as childless`() {
        val leafAccount = RecordingAccountService.accountDto(SystemAccount.CASH, accountIsActive = true)
        `when`(accountCache.getAll()).thenReturn(listOf(leafAccount))
        `when`(accountRepository.existsByParentAccountCode(SystemAccount.CASH.code)).thenReturn(false)

        assertFalse(accountDataFetcher.getByCode(SystemAccount.CASH.code).hasChildren)
    }

    @Test
    fun `selection candidates are built from the cached accounts`() {
        val cash = RecordingAccountService.accountDto(SystemAccount.CASH, accountIsActive = true)
        `when`(accountCache.getAll()).thenReturn(listOf(cash))

        assertEquals(listOf(cash.code), accountDataFetcher.getSelectionCandidates().map { it.code })
    }
}
