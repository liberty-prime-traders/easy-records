package app.myliberty.easy_records.organizations.business.core.ledger

import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerEntryRequest
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class LedgerEntryRequestTest {

    @Test
    fun `a zero amount leg is omitted rather than posted`() {
        assertTrue(LedgerEntryRequest.debitUnlessZero(SystemAccount.GROSS_SALES.code, BigDecimal("0.0000")).isEmpty())
        assertTrue(LedgerEntryRequest.creditUnlessZero(SystemAccount.GROSS_SALES.code, BigDecimal.ZERO).isEmpty())
    }

    @Test
    fun `a positive amount leg is built with the requested side`() {
        assertEquals(
            listOf(LedgerEntryRequest(SystemAccount.CASH.code, EntryType.DEBIT, PositiveMoney.of(BigDecimal("10.00")))),
            LedgerEntryRequest.debitUnlessZero(SystemAccount.CASH.code, BigDecimal("10.0000"))
        )
    }

    @Test
    fun `a negative amount is not skipped and fails loudly so a bad total cannot post silently`() {
        assertThrows(LibertyGenericException::class.java) {
            LedgerEntryRequest.debitUnlessZero(SystemAccount.COST_OF_GOODS_SOLD.code, BigDecimal("-1.00"))
        }
    }

    @Test
    fun `an amount finer than the money scale is rejected even when the entries would balance`() {
        assertThrows(LibertyGenericException::class.java) { PositiveMoney.of(BigDecimal("10.005")) }
    }
}
