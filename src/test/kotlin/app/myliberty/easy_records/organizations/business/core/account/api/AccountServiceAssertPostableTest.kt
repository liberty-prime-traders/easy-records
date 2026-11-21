package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccountServiceAssertPostableTest {

    private val accountService = RecordingAccountService().accountService

    @Test
    fun `leaf active accounts are postable`() {
        assertDoesNotThrow {
            accountService.assertPostable(
                listOf(
                    debit(SystemAccount.CASH.code, BigDecimal.TEN),
                    credit(SystemAccount.GROSS_SALES.code, BigDecimal.TEN)
                )
            )
        }
    }

    @Test
    fun `unknown account codes are rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            accountService.assertPostable(listOf(debit("999.999", BigDecimal.TEN)))
        }
    }

    @Test
    fun `accounts with children are rejected`() {
        assertThrows(LibertyGenericException::class.java) {
            accountService.assertPostable(listOf(debit(SystemAccount.ACCOUNTS_RECEIVABLE.code, BigDecimal.TEN)))
        }
    }

    @Test
    fun `an inactive account may be drawn down but not built up`() {
        val accountDtos = RecordingAccountService.systemAccountDtos()
            .map { if (it.code == SystemAccount.CASH.code) it.copy(accountIsActive = false) else it }
        val inactiveCashService = RecordingAccountService(accountDtos).accountService

        assertThrows(LibertyGenericException::class.java) {
            inactiveCashService.assertPostable(listOf(debit(SystemAccount.CASH.code, BigDecimal.TEN)))
        }
        assertDoesNotThrow {
            inactiveCashService.assertPostable(listOf(credit(SystemAccount.CASH.code, BigDecimal.TEN)))
        }
    }

    @Test
    fun `a reversal may build up an inactive account but still never posts to one with children`() {
        val accountDtos = RecordingAccountService.systemAccountDtos()
            .map { if (it.code == SystemAccount.CASH.code) it.copy(accountIsActive = false) else it }
        val inactiveCashService = RecordingAccountService(accountDtos).accountService

        assertDoesNotThrow {
            inactiveCashService.assertPostableReversal(listOf(debit(SystemAccount.CASH.code, BigDecimal.TEN)))
        }
        assertThrows(LibertyGenericException::class.java) {
            inactiveCashService.assertPostableReversal(listOf(debit(SystemAccount.ACCOUNTS_RECEIVABLE.code, BigDecimal.TEN)))
        }
    }

    @Test
    fun `direction is judged against the account's own normal balance`() {
        val accountDtos = RecordingAccountService.systemAccountDtos()
            .map { if (it.code == SystemAccount.SALES_DISCOUNTS.code) it.copy(accountIsActive = false) else it }
        val inactiveDiscountsService = RecordingAccountService(accountDtos).accountService

        assertThrows(LibertyGenericException::class.java) {
            inactiveDiscountsService.assertPostable(listOf(debit(SystemAccount.SALES_DISCOUNTS.code, BigDecimal.TEN)))
        }
        assertDoesNotThrow {
            inactiveDiscountsService.assertPostable(listOf(credit(SystemAccount.SALES_DISCOUNTS.code, BigDecimal.TEN)))
        }
    }

    private fun debit(accountCode: String, amount: BigDecimal) = AccountPosting(accountCode, amount, EntryType.DEBIT)

    private fun credit(accountCode: String, amount: BigDecimal) = AccountPosting(accountCode, amount, EntryType.CREDIT)
}
