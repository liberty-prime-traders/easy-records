package app.myliberty.easy_records.organizations.business.ledger

import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.events.SaleCostRestatedEvent
import app.myliberty.easy_records.organizations.business.account.api.EntryType
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.organizations.business.ledger.processors.SaleCogsRestatementAccountingProcessor
import app.myliberty.easy_records.util.business.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class SaleCogsRestatementEntriesTest {

    private val saleCogsRestatementAccountingProcessor = SaleCogsRestatementAccountingProcessor(
        mock(LedgerPostingGate::class.java), mock(LedgerPostingService::class.java)
    )

    private fun event(oldCostTotal: String, newCostTotal: String) = SaleCostRestatedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1"),
        timestamp = Instant.now(),
        correlationId = null,
        stockRevisionId = UUID.randomUUID(),
        stockRevisionReferenceNumber = "STRV000001",
        oldCostTotal = Money.of(BigDecimal(oldCostTotal)),
        newCostTotal = Money.of(BigDecimal(newCostTotal)),
        postingDate = LocalDate.parse("2026-11-08")
    )

    private fun entriesOf(oldCostTotal: String, newCostTotal: String) =
        saleCogsRestatementAccountingProcessor.prepareLedgerRequest(event(oldCostTotal, newCostTotal))
            .entries.map { Triple(it.accountCode, it.entryType, it.amount.toBigDecimal()) }

    @Test
    fun `a cost that rises debits cost of goods sold and credits inventory by the change`() {
        assertEquals(
            listOf(
                Triple(SystemAccount.COST_OF_GOODS_SOLD.code, EntryType.DEBIT, BigDecimal("12.00")),
                Triple(SystemAccount.INVENTORY.code, EntryType.CREDIT, BigDecimal("12.00")),
            ),
            entriesOf("204.00", "216.00")
        )
    }

    @Test
    fun `a cost that falls debits inventory and credits cost of goods sold by the change`() {
        assertEquals(
            listOf(
                Triple(SystemAccount.INVENTORY.code, EntryType.DEBIT, BigDecimal("2.55")),
                Triple(SystemAccount.COST_OF_GOODS_SOLD.code, EntryType.CREDIT, BigDecimal("2.55")),
            ),
            entriesOf("147.90", "145.35")
        )
    }

    @Test
    fun `the posting is a restatement keyed on the stock revision`() {
        val ledgerPostingRequest = saleCogsRestatementAccountingProcessor.prepareLedgerRequest(event("1.00", "2.00"))

        assertEquals("STRV000001", ledgerPostingRequest.sourceReferenceNumber)
        assertEquals(LedgerSourceType.SALE_COGS_RESTATEMENT, ledgerPostingRequest.sourceType)
        assertEquals(LocalDate.parse("2026-11-08"), ledgerPostingRequest.postingDate)
    }
}
