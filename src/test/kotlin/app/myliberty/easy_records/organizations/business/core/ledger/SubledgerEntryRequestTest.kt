package app.myliberty.easy_records.organizations.business.core.ledger

import app.myliberty.easy_records.organizations.business.core.ledger.api.SubledgerEntryRequest
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@WithDefaultOrganizationSession
class SubledgerEntryRequestTest {

    @Test
    fun `a subledger amount is never negative, so a reversal cannot be posted as a negative column`() {
        assertThrows(RtsGenericException::class.java) {
            SubledgerEntryRequest("CT000001", payableAmount = Money.of(BigDecimal("-1.00")), receivableAmount = Money.ZERO)
        }
    }

    @Test
    fun `the unused column is Money ZERO`() {
        val subledgerEntryRequest = SubledgerEntryRequest("CT000001", payableAmount = Money.of(BigDecimal("5.00")), receivableAmount = Money.ZERO)

        assertEquals(Money.ZERO, subledgerEntryRequest.receivableAmount)
    }
}
