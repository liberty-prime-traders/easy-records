package app.myliberty.easy_records.organizations.business.core.account.api

import app.myliberty.easy_records.organizations.business.core.account.AccountCache
import app.myliberty.easy_records.organizations.business.core.account.AccountDto
import app.myliberty.easy_records.organizations.business.core.account.AccountRepository
import app.myliberty.easy_records.organizations.business.core.account.AccountResponseBuilder
import app.myliberty.easy_records.organizations.business.core.account.ChildAccountCreator
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.OffsetDateTime
import java.util.UUID

class AccountServiceCreateRootTest {

    private val existingLoansRoot = AccountDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        code = "100",
        name = "Equipment Loans",
        accountType = AccountType.LIABILITY,
        currencyCode = "KES",
        accountIsActive = true,
        accountIsSystemMaintained = false,
        parentAccountCode = null
    )
    private val accountCache = mock(AccountCache::class.java).also { accountCache ->
        `when`(accountCache.getAll()).thenReturn(listOf(existingLoansRoot))
    }
    private val accountService = AccountService(
        accountCache,
        mock(AccountRepository::class.java),
        mock(AccountResponseBuilder::class.java),
        mock(ChildAccountCreator::class.java),
        mock(AccountStructureLock::class.java),
        mock(AccountUsagesFinder::class.java)
    )

    @Test
    fun `a contra type is refused as a root account`() {
        val thrown = assertThrows(LibertyGenericException::class.java) {
            val contraRootRequest = AccountRootCreateRequest(name = "Discounts", accountType = AccountType.ASSET_CONTRA)
            accountService.createRoot(contraRootRequest)
        }

        assertEquals(
            "Only certain account types can be created as root accounts and ASSET_CONTRA is not one of them",
            thrown.message
        )
    }

    @Test
    fun `a root account name already in use is refused`() {
        val thrown = assertThrows(LibertyGenericException::class.java) {
            val duplicateNameRequest = AccountRootCreateRequest(
                name = "Equipment Loans",
                accountType = AccountType.ASSET
            )
            accountService.createRoot(duplicateNameRequest)
        }

        assertEquals("A root account with the same name already exists", thrown.message)
    }
}
