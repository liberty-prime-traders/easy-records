package app.myliberty.easy_records.organizations.business.core.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import app.myliberty.easy_records.organizations.business.core.account.api.AccountType
import java.time.OffsetDateTime
import java.util.UUID

class AccountDtoTest {

    @Test
    fun `a segment of all zeros displays as 0`() {
        assertEquals("0.1", accountDto("000.001").toDisplayCode())
    }

    @Test
    fun `leading zeros are dropped from each segment of the display code`() {
        assertEquals("1.1.5.1", accountDto("001.001.005.001").toDisplayCode())
    }

    private fun accountDto(code: String) = AccountDto(
        id = UUID.randomUUID(),
        createdById = UUID.randomUUID(),
        createdOn = OffsetDateTime.now(),
        code = code,
        name = "Account",
        accountType = AccountType.ASSET,
        currencyCode = "KES",
        accountIsActive = true,
        accountIsSystemMaintained = false,
        parentAccountCode = null
    )
}
