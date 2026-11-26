package app.myliberty.easy_records.organizations.business.core.account

import app.myliberty.easy_records.organizations.business.core.account.api.AccountChildCreateRequest
import app.myliberty.easy_records.organizations.business.core.account.api.AccountInsertDto
import app.myliberty.easy_records.organizations.business.core.account.api.AccountType
import app.myliberty.easy_records.organizations.business.core.account.api.AccountUsagesFinder
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.OffsetDateTime
import java.util.UUID

class ChildAccountCreatorTest {

    private val accountCache = mock(AccountCache::class.java)
    private val accountUsagesFinder = mock(AccountUsagesFinder::class.java)
    private val childAccountCreator = ChildAccountCreator(accountCache, accountUsagesFinder)

    private val currentAssets = accountDto(
        "001.001", "Current Assets", AccountType.ASSET, "001", systemMaintained = true
    )
    private val digitalPayments = accountDto(
        SystemAccount.DIGITAL_PAYMENTS.code, "Digital Payments", AccountType.ASSET, "001.001", systemMaintained = true
    )
    private val cash = accountDto(
        SystemAccount.CASH.code, "Cash", AccountType.ASSET, "001.001", systemMaintained = true
    )
    private val equipmentLoans = accountDto("100", "Equipment Loans", AccountType.LIABILITY, null)

    @Test
    fun `a child of an inactive account is refused`() {
        val inactiveEquipmentLoans = equipmentLoans.copy(accountIsActive = false)

        val thrown = assertThrows(LibertyGenericException::class.java) {
            childAccountCreator.createChild(childRequest("100", "Bank Loans"), accountsByCode(inactiveEquipmentLoans))
        }

        assertEquals("Cannot add children to inactive account 'Equipment Loans' (100)", thrown.message)
    }

    @Test
    fun `a child of a system account that takes no children is refused`() {
        val thrown = assertThrows(LibertyGenericException::class.java) {
            childAccountCreator.createChild(childRequest("001.001.001", "Savings"), accountsByCode(currentAssets, cash))
        }

        assertEquals(
            "Cash (1.1.1) is maintained by the system and does not allow child accounts to be added",
            thrown.message
        )
    }

    @Test
    fun `a child beneath a child of Digital Payments is refused`() {
        val bank = accountDto("001.001.005.001", "Bank", AccountType.ASSET, digitalPayments.code)

        val thrown = assertThrows(LibertyGenericException::class.java) {
            childAccountCreator.createChild(
                childRequest("001.001.005.001", "Savings"),
                accountsByCode(currentAssets, digitalPayments, bank)
            )
        }

        assertEquals("Cannot add further children under 'Bank (1.1.5.1)'", thrown.message)
    }

    @Test
    fun `a sibling with the same name is refused`() {
        val bank = accountDto("001.001.005.001", "Bank", AccountType.ASSET, digitalPayments.code)

        val thrown = assertThrows(LibertyGenericException::class.java) {
            childAccountCreator.createChild(
                childRequest(digitalPayments.code, "Bank"),
                accountsByCode(currentAssets, digitalPayments, bank)
            )
        }

        assertEquals("Sibling is already registered with the same name", thrown.message)
    }

    @Test
    fun `a new child takes the next code under its parent and the parent's type`() {
        childAccountCreator.createChild(childRequest("100", "Bank Loans"), accountsByCode(equipmentLoans))

        verify(accountCache).create(
            AccountInsertDto(
                code = "100.001",
                name = "Bank Loans",
                accountType = AccountType.LIABILITY,
                currencyCode = "KES",
                parentAccountCode = "100"
            )
        )
    }

    private fun childRequest(parentAccountCode: String, name: String) =
        AccountChildCreateRequest(parentAccountCode = parentAccountCode, name = name)

    private fun accountsByCode(vararg accounts: AccountDto): Map<String, AccountDto> =
        accounts.associateBy { it.code }

    private fun accountDto(
        code: String,
        name: String,
        accountType: AccountType,
        parentAccountCode: String?,
        systemMaintained: Boolean = false
    ) = AccountDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        code = code,
        name = name,
        accountType = accountType,
        currencyCode = "KES",
        accountIsActive = true,
        accountIsSystemMaintained = systemMaintained,
        parentAccountCode = parentAccountCode
    )
}
