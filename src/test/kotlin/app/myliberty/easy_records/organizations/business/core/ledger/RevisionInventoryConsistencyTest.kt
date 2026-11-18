package app.myliberty.easy_records.organizations.business.core.ledger

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveryRevisedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.SaleCostRestatedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockAdjustmentCostRestatedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.StockAdjustmentCostRestatementDto
import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.organizations.business.core.ledger.api.PostedStockAdjustmentAccounts
import app.myliberty.easy_records.organizations.business.core.ledger.api.PostedStockAdjustmentAccountsReader
import app.myliberty.easy_records.organizations.business.core.ledger.processors.PurchaseDeliveryRevisionAccountingProcessor
import app.myliberty.easy_records.organizations.business.core.ledger.processors.SaleCogsRestatementAccountingProcessor
import app.myliberty.easy_records.organizations.business.core.ledger.processors.StockAdjustmentRestatementAccountingProcessor
import app.myliberty.easy_records.util.business.Money
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// D25: after a revision the ledger's inventory moves by exactly what the stock on hand is worth less than before.
// On-hand value changes by the line total change minus the change in what the consumers drew from it.
class RevisionInventoryConsistencyTest {

    private val supplierId = UUID.randomUUID()
    private val locationContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1")
    private val postedStockAdjustmentAccountsReader = mock(PostedStockAdjustmentAccountsReader::class.java).also {
        `when`(it.getByAdjustmentReferenceNumber(setOf("ADJ1", "ADJ2"))).thenReturn(
            mapOf(
                "ADJ1" to PostedStockAdjustmentAccounts(
                    SystemAccount.INTERNAL_USE_EXPENSE.code, SystemAccount.INVENTORY.code
                ),
                "ADJ2" to PostedStockAdjustmentAccounts(
                    SystemAccount.SHRINKAGE_AND_LOSSES.code, SystemAccount.INVENTORY.code
                )
            )
        )
    }
    private val purchaseDeliveryRevisionProcessor = PurchaseDeliveryRevisionAccountingProcessor(
        mock(ContactService::class.java).also { contactService ->
            val supplierDto = mock(ContactDto::class.java)
            `when`(supplierDto.referenceNumber).thenReturn("SUPPLIER-1")
            `when`(contactService.getContactById(supplierId)).thenReturn(supplierDto)
        },
        mock(LedgerPostingGate::class.java),
        mock(LedgerPostingService::class.java)
    )
    private val saleCogsRestatementProcessor = SaleCogsRestatementAccountingProcessor(
        mock(LedgerPostingGate::class.java), mock(LedgerPostingService::class.java)
    )
    private val stockAdjustmentRestatementProcessor = StockAdjustmentRestatementAccountingProcessor(
        mock(LedgerPostingGate::class.java), postedStockAdjustmentAccountsReader, mock(LedgerPostingService::class.java)
    )

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    private fun inventoryDebitMinusCredit(ledgerPostingRequest: LedgerPostingRequest): BigDecimal =
        ledgerPostingRequest.entries
            .filter { it.accountCode == SystemAccount.INVENTORY.code }
            .sumOf {
                if (it.entryType == EntryType.DEBIT) it.amount.toBigDecimal() else it.amount.toBigDecimal().negate()
            }

    private fun money(value: String) = Money.of(BigDecimal(value))

    // Each consumer is (old cost, new cost). The adjustments are internal use and shrinkage.
    private fun assertInventoryFollowsStockValue(
        revisedLineTotal: String,
        replacementLineTotal: String,
        saleCosts: Pair<String, String>,
        internalUseCosts: Pair<String, String>,
        shrinkageCosts: Pair<String, String>
    ) {
        val purchaseRequest = purchaseDeliveryRevisionProcessor.prepareLedgerRequest(
            PurchaseDeliveryRevisedEvent(
                eventId = UUID.randomUUID(), sourceContext = locationContext, timestamp = Instant.now(),
                correlationId = null, stockRevisionId = UUID.randomUUID(), stockRevisionReferenceNumber = "STRV000001",
                purchaseReferenceNumber = "PCH-1", supplierId = supplierId,
                revisedLineTotal = money(revisedLineTotal), replacementLineTotal = money(replacementLineTotal),
                postingDate = LocalDate.parse("2026-11-08")
            )
        )
        val saleRequest = saleCogsRestatementProcessor.prepareLedgerRequest(
            SaleCostRestatedEvent(
                eventId = UUID.randomUUID(), sourceContext = locationContext, timestamp = Instant.now(),
                correlationId = null, stockRevisionId = UUID.randomUUID(), stockRevisionReferenceNumber = "STRV000001",
                oldCostTotal = money(saleCosts.first), newCostTotal = money(saleCosts.second),
                postingDate = LocalDate.parse("2026-11-08")
            )
        )
        val adjustmentRequest = stockAdjustmentRestatementProcessor.prepareLedgerRequest(
            StockAdjustmentCostRestatedEvent(
                eventId = UUID.randomUUID(), sourceContext = locationContext, timestamp = Instant.now(),
                correlationId = null, stockRevisionId = UUID.randomUUID(), stockRevisionReferenceNumber = "STRV000001",
                restatements = listOf(
                    StockAdjustmentCostRestatementDto(
                        "ADJ1", "RULE-DR", "RULE-CR", money(internalUseCosts.first), money(internalUseCosts.second)
                    ),
                    StockAdjustmentCostRestatementDto(
                        "ADJ2", "RULE-DR", "RULE-CR", money(shrinkageCosts.first), money(shrinkageCosts.second)
                    )
                ),
                postingDate = LocalDate.parse("2026-11-08")
            )
        )

        val consumerCostChange = listOf(saleCosts, internalUseCosts, shrinkageCosts)
            .sumOf { BigDecimal(it.second).subtract(BigDecimal(it.first)) }
        val stockValueChange = BigDecimal(replacementLineTotal).subtract(BigDecimal(revisedLineTotal))
            .subtract(consumerCostChange)
        val ledgerInventoryChange = listOf(purchaseRequest, saleRequest, adjustmentRequest)
            .sumOf { inventoryDebitMinusCredit(it) }

        assertEquals(
            0,
            stockValueChange.compareTo(ledgerInventoryChange),
            "stock $stockValueChange, ledger $ledgerInventoryChange"
        )
    }

    @Test
    fun `a dearer delivery that raises every consumer's cost keeps inventory equal to stock value`() {
        assertInventoryFollowsStockValue(
            "100.00", "120.00", "40.00" to "48.00", "10.00" to "12.00", "5.00" to "6.00"
        )
    }

    @Test
    fun `a cheaper delivery that lowers every consumer's cost keeps inventory equal to stock value`() {
        assertInventoryFollowsStockValue(
            "100.00", "80.00", "40.00" to "32.00", "10.00" to "8.00", "5.00" to "4.00"
        )
    }

    @Test
    fun `a smaller delivery that raises some costs and lowers others keeps inventory equal to stock value`() {
        assertInventoryFollowsStockValue(
            "100.00", "70.00", "40.00" to "35.00", "10.00" to "13.50", "5.00" to "5.00"
        )
    }
}
