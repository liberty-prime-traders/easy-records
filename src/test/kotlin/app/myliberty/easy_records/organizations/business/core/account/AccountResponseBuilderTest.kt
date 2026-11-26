package app.myliberty.easy_records.organizations.business.core.account

import app.myliberty.easy_records.organizations.business.core.account.api.AccountType
import app.myliberty.easy_records.organizations.business.core.account.api.RecordingAccountService
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.opening_balance.api.OpeningBalanceReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anySet
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class AccountResponseBuilderTest {

    private val accountRepository = mock(AccountRepository::class.java)
    private val openingBalanceReader = mock(OpeningBalanceReader::class.java)
    private val accountResponseBuilder = AccountResponseBuilder(accountRepository, openingBalanceReader)

    @Test
    fun `contra children are subtracted from their parent in the rolled up balance`() {
        stubLeafBalances(
            SystemAccount.GROSS_SALES to "100.00",
            SystemAccount.SALES_DISCOUNTS to "10.00",
            SystemAccount.SALES_RETURNS to "5.00",
            SystemAccount.TRADE_RECEIVABLES to "90.00",
            SystemAccount.ALLOWANCE_FOR_DOUBTFUL_ACCOUNTS to "10.00"
        )

        val responsesByCode = accountResponseBuilder.buildResponse(RecordingAccountService.systemAccountDtos()).associateBy { it.code }

        assertEquals(0, BigDecimal("85.00").compareTo(responsesByCode.getValue(SystemAccount.SALES_REVENUE.code).currentBalance))
        assertEquals(0, BigDecimal("85.00").compareTo(responsesByCode.getValue(SystemAccount.REVENUE.code).currentBalance))
        assertEquals(0, BigDecimal("80.00").compareTo(responsesByCode.getValue(SystemAccount.ACCOUNTS_RECEIVABLE.code).currentBalance))
    }

    @Test
    fun `opening balances roll up the same way`() {
        stubLeafBalances()
        `when`(openingBalanceReader.getAmountsByAccountCodes(anySet())).thenReturn(
            mapOf(
                SystemAccount.GROSS_SALES.code to BigDecimal("100.00"),
                SystemAccount.SALES_DISCOUNTS.code to BigDecimal("10.00")
            )
        )

        val responsesByCode = accountResponseBuilder.buildResponse(RecordingAccountService.systemAccountDtos()).associateBy { it.code }

        assertEquals(0, BigDecimal("90.00").compareTo(responsesByCode.getValue(SystemAccount.SALES_REVENUE.code).openingBalance))
    }

    private fun stubLeafBalances(vararg balancesBySystemAccount: Pair<SystemAccount, String>) {
        val balances = balancesBySystemAccount.map { (systemAccount, amount) ->
            object : AccountBalance {
                override val code = systemAccount.code
                override val currentBalance = BigDecimal(amount)
            }
        }
        `when`(accountRepository.findBalancesByCodes(anySet())).thenReturn(balances)
        `when`(openingBalanceReader.getAmountsByAccountCodes(anySet())).thenReturn(emptyMap())
    }

    @Test
    fun `a system account missing from the system account list is not extensible`() {
        stubLeafBalances()
        val unlisted = responseAccount("001.099", AccountType.ASSET, "001", systemMaintained = true)

        val response = accountResponseBuilder.buildResponse(listOf(unlisted)).single()

        assertFalse(response.accountIsExtensible)
    }

    @Test
    fun `an account under a single-level extension point is not itself extensible`() {
        stubLeafBalances()
        val digitalPayments = RecordingAccountService.accountDto(SystemAccount.DIGITAL_PAYMENTS, accountIsActive = true)
        val paymentAccount = responseAccount("001.001.005.001", AccountType.ASSET, "001.001.005")

        val responsesByCode = accountResponseBuilder.buildResponse(listOf(digitalPayments, paymentAccount)).associateBy { it.code }

        assertTrue(responsesByCode.getValue("001.001.005").accountIsExtensible)
        assertFalse(responsesByCode.getValue("001.001.005.001").accountIsExtensible)
    }

    @Test
    fun `a user root account can gain children`() {
        stubLeafBalances()
        val userRoot = responseAccount("100", AccountType.LIABILITY, parentAccountCode = null)

        assertTrue(accountResponseBuilder.buildResponse(listOf(userRoot)).single().accountIsExtensible)
    }

    @Test
    fun `a user account under a user account can gain children`() {
        stubLeafBalances()
        val userParent = responseAccount("100", AccountType.LIABILITY, parentAccountCode = null)
        val userChild = responseAccount("100.001", AccountType.LIABILITY, parentAccountCode = "100")

        val responsesByCode = accountResponseBuilder.buildResponse(listOf(userParent, userChild)).associateBy { it.code }

        assertTrue(responsesByCode.getValue("100.001").accountIsExtensible)
    }

    @Test
    fun `a user account under a system account that takes no children can gain children`() {
        stubLeafBalances()
        val currentAssets = RecordingAccountService.accountDto(SystemAccount.CURRENT_ASSETS, accountIsActive = true)
        val userUnderCurrentAssets = responseAccount("001.001.009", AccountType.ASSET, parentAccountCode = "001.001")

        val responsesByCode = accountResponseBuilder.buildResponse(listOf(currentAssets, userUnderCurrentAssets)).associateBy { it.code }

        assertTrue(responsesByCode.getValue("001.001.009").accountIsExtensible)
    }

    @Test
    fun `a child shows its parent's display label and a root shows none`() {
        stubLeafBalances()
        val userParent = responseAccount("100", AccountType.LIABILITY, parentAccountCode = null)
        val userChild = responseAccount("100.001", AccountType.LIABILITY, parentAccountCode = "100")

        val responsesByCode = accountResponseBuilder.buildResponse(listOf(userParent, userChild)).associateBy { it.code }

        assertEquals(userParent.label, responsesByCode.getValue("100.001").parentAccount)
        assertEquals(null, responsesByCode.getValue("100").parentAccount)
    }

    private fun responseAccount(
        code: String,
        accountType: AccountType,
        parentAccountCode: String?,
        systemMaintained: Boolean = false
    ) = AccountDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        code = code,
        name = "Account $code",
        accountType = accountType,
        currencyCode = "KES",
        accountIsActive = true,
        accountIsSystemMaintained = systemMaintained,
        parentAccountCode = parentAccountCode
    )
}
