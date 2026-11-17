package app.myliberty.easy_records.organizations.business.ledger

import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class LedgerEntriesValidatorTest {

    @Test
    fun `balanced entries pass`() {
        assertDoesNotThrow {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, PositiveMoney.of(BigDecimal("10.00"))),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, PositiveMoney.of(BigDecimal("10.00")))
                )
            )
        }
    }

    @Test
    fun `no entries is balanced so a fully free sale still saves its group for idempotency`() {
        assertDoesNotThrow { LedgerEntriesValidator.validate(emptyList()) }
    }

    @Test
    fun `unbalanced entries are rejected`() {
        assertThrows(RtsGenericException::class.java) {
            LedgerEntriesValidator.validate(
                listOf(
                    LedgerEntryRequest.debit(SystemAccount.CASH.code, PositiveMoney.of(BigDecimal("10.00"))),
                    LedgerEntryRequest.credit(SystemAccount.GROSS_SALES.code, PositiveMoney.of(BigDecimal("9.00")))
                )
            )
        }
    }
}
