package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.organizations.business.core.account.AccountBalance
import app.myliberty.easy_records.organizations.business.core.account.AccountEntity
import app.myliberty.easy_records.organizations.business.core.account.AccountRepository
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import java.lang.reflect.Proxy
import java.math.BigDecimal

class DenormalizedYearEndBalanceTransferEdgeCasesTest {

    private var lockAcquisitions = 0
    private val incrementsByAccountCode = mutableMapOf<String, BigDecimal>()

    private val accountStructureLock = mock(AccountStructureLock::class.java).also { lock ->
        doAnswer { lockAcquisitions++ }.`when`(lock).acquire(anyCollection())
    }

    @Test
    fun `a missing Retained Earnings account is refused before any lock or balance change`() {
        val repository = repositoryWith(listOf(entity(SystemAccount.GROSS_SALES)), emptyMap())

        val thrown = assertThrows(LibertyGenericException::class.java) {
            DenormalizedYearEndBalanceTransfer(repository, accountStructureLock).applyYearEndBalanceTransfer()
        }

        assertEquals("Retained Earnings account not found in organization.", thrown.message)
        assertEquals(0, lockAcquisitions)
        assertTrue(incrementsByAccountCode.isEmpty())
    }

    @Test
    fun `with no revenue or expense accounts there is nothing to close and no lock is taken`() {
        val repository = repositoryWith(
            listOf(entity(SystemAccount.RETAINED_EARNINGS), entity(SystemAccount.CASH)),
            emptyMap()
        )

        DenormalizedYearEndBalanceTransfer(repository, accountStructureLock).applyYearEndBalanceTransfer()

        assertEquals(0, lockAcquisitions)
        assertTrue(incrementsByAccountCode.isEmpty())
    }

    @Test
    fun `closing accounts that all carry zero balances are locked but not moved`() {
        val repository = repositoryWith(
            listOf(entity(SystemAccount.RETAINED_EARNINGS), entity(SystemAccount.GROSS_SALES)),
            emptyMap()
        )

        DenormalizedYearEndBalanceTransfer(repository, accountStructureLock).applyYearEndBalanceTransfer()

        assertEquals(1, lockAcquisitions)
        assertTrue(incrementsByAccountCode.isEmpty())
    }

    @Test
    fun `a net loss is closed into retained earnings as a negative amount`() {
        val repository = repositoryWith(
            listOf(
                entity(SystemAccount.RETAINED_EARNINGS),
                entity(SystemAccount.GROSS_SALES),
                entity(SystemAccount.TAX_EXPENSE)
            ),
            mapOf(SystemAccount.GROSS_SALES.code to BigDecimal("10.00"), SystemAccount.TAX_EXPENSE.code to BigDecimal("30.00"))
        )

        DenormalizedYearEndBalanceTransfer(repository, accountStructureLock).applyYearEndBalanceTransfer()

        assertEquals(0, BigDecimal("-20.00").compareTo(incrementsByAccountCode.getValue(SystemAccount.RETAINED_EARNINGS.code)))
    }

    private fun entity(systemAccount: SystemAccount) = AccountEntity(
        code = systemAccount.code,
        name = systemAccount.accountName,
        accountType = systemAccount.type,
        currencyCode = "USD",
        accountIsActive = true,
        accountIsSystemMaintained = true,
        parentAccountCode = systemAccount.parent?.code
    )

    private fun repositoryWith(accounts: List<AccountEntity>, balancesByCode: Map<String, BigDecimal>): AccountRepository =
        Proxy.newProxyInstance(
            AccountRepository::class.java.classLoader,
            arrayOf(AccountRepository::class.java)
        ) { _, method, arguments ->
            when (method.name) {
                "findAll" -> accounts
                "findBalancesByCodes" -> {
                    @Suppress("UNCHECKED_CAST")
                    (arguments[0] as Set<String>).map { requestedCode ->
                        object : AccountBalance {
                            override val code = requestedCode
                            override val currentBalance = balancesByCode[requestedCode] ?: BigDecimal.ZERO
                        }
                    }
                }
                "incrementBalance" -> {
                    incrementsByAccountCode.merge(arguments[0] as String, arguments[1] as BigDecimal, BigDecimal::add)
                    null
                }
                else -> null
            }
        } as AccountRepository
}
