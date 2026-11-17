package app.myliberty.easy_records.organizations.business.core.account_selection.api

import app.myliberty.easy_records.organizations.business.core.account.api.AccountDataFetcher
import app.myliberty.easy_records.organizations.business.core.account.api.AccountSelectionCandidate
import app.myliberty.easy_records.organizations.business.core.account.api.AccountStructureLock
import app.myliberty.easy_records.organizations.business.core.account.api.RecordingAccountService
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

// A fresh read taken before the lock would let createChild or toggleActive change the account between check and save.
class AccountSelectionServiceTest {

    private val accountDataFetcher = mock(AccountDataFetcher::class.java)
    private val accountStructureLock = mock(AccountStructureLock::class.java)
    private val accountSelectionService = AccountSelectionService(accountDataFetcher, accountStructureLock)

    @Test
    fun `requireSelectable takes the account lock before reading fresh candidates`() {
        `when`(accountDataFetcher.getFreshSelectionCandidates()).thenReturn(candidatesOf(activeAccounts = true))

        accountSelectionService.requireSelectable(AccountSelectionRule.EXPENSE_TYPE, SystemAccount.RENT_EXPENSE.code)

        val lockThenFreshRead = inOrder(accountStructureLock, accountDataFetcher)
        lockThenFreshRead.verify(accountStructureLock).acquire(SystemAccount.RENT_EXPENSE.code)
        lockThenFreshRead.verify(accountDataFetcher).getFreshSelectionCandidates()
    }

    @Test
    fun `requireSelectable refuses unknown and inactive accounts before consulting the rule`() {
        `when`(accountDataFetcher.getFreshSelectionCandidates()).thenReturn(candidatesOf(activeAccounts = false))

        assertThrows(LibertyGenericException::class.java) {
            accountSelectionService.requireSelectable(AccountSelectionRule.EXPENSE_TYPE, "999.999")
        }
        assertThrows(LibertyGenericException::class.java) {
            accountSelectionService.requireSelectable(AccountSelectionRule.EXPENSE_TYPE, SystemAccount.RENT_EXPENSE.code)
        }
    }

    private fun candidatesOf(activeAccounts: Boolean): List<AccountSelectionCandidate> =
        AccountSelectionCandidate.fromAccounts(
            SystemAccount.entries.map { RecordingAccountService.accountDto(it, accountIsActive = activeAccounts) }
        )
}
