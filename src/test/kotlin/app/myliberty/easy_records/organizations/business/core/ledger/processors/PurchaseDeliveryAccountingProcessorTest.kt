package app.myliberty.easy_records.organizations.business.core.ledger.processors

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import app.myliberty.easy_records.organizations.business.core.account.api.EntryType
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.ledger.LedgerEntryGroupRepository
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.util.business.Money
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PurchaseDeliveryAccountingProcessorTest {

    private val supplierId = UUID.randomUUID()
    private val locationId = UUID.randomUUID()
    private val contactService = mock(ContactService::class.java).also { contactService ->
        val supplierDto = mock(ContactDto::class.java)
        `when`(supplierDto.referenceNumber).thenReturn("SUPPLIER-1")
        `when`(contactService.getContactById(supplierId)).thenReturn(supplierDto)
    }
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val purchaseDeliveryAccountingProcessor = PurchaseDeliveryAccountingProcessor(
        contactService,
        ledgerEntryGroupRepository,
        mock(LedgerPostingService::class.java)
    )

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "Pacific/Auckland"),
                location = LocationSession(id = locationId, schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a delivery debits inventory and credits trade payables by the delivery total`() {
        val ledgerPostingRequest = purchaseDeliveryAccountingProcessor.prepareLedgerRequest(deliveredEvent("20.00"))

        assertEquals("DEL-1", ledgerPostingRequest.sourceReferenceNumber)
        assertEquals(LedgerSourceType.PURCHASE_DELIVERY, ledgerPostingRequest.sourceType)
        assertFalse(ledgerPostingRequest.reversal)
        val entryLines = ledgerPostingRequest.entries.map { entry ->
            Triple(entry.accountCode, entry.entryType, entry.amount.toBigDecimal().toPlainString())
        }
        assertEquals(
            listOf(
                Triple(SystemAccount.INVENTORY.code, EntryType.DEBIT, "20.00"),
                Triple(SystemAccount.TRADE_PAYABLES.code, EntryType.CREDIT, "20.00")
            ),
            entryLines
        )
    }

    @Test
    fun `the supplier is owed the delivery total in the payable column only`() {
        val ledgerPostingRequest = purchaseDeliveryAccountingProcessor.prepareLedgerRequest(deliveredEvent("20.00"))

        val subledgerEntry = ledgerPostingRequest.subledgerEntries.single()
        assertEquals("SUPPLIER-1", subledgerEntry.contactReferenceNumber)
        assertEquals(0, BigDecimal("20.00").compareTo(subledgerEntry.payableAmount.toBigDecimal()))
        assertEquals(0, BigDecimal.ZERO.compareTo(subledgerEntry.receivableAmount.toBigDecimal()))
    }

    @Test
    fun `a zero-value delivery posts no legs but still records the group`() {
        val ledgerPostingRequest = purchaseDeliveryAccountingProcessor.prepareLedgerRequest(deliveredEvent("0.00"))

        assertTrue(ledgerPostingRequest.entries.isEmpty())
        assertEquals(1, ledgerPostingRequest.subledgerEntries.size)
        assertEquals(LedgerSourceType.PURCHASE_DELIVERY, ledgerPostingRequest.sourceType)
    }

    @Test
    fun `the posting date is the delivery date in the organization zone, not in UTC`() {
        val deliveredAtLateUtcEvening = OffsetDateTime.of(2026, 3, 10, 20, 0, 0, 0, ZoneOffset.UTC)

        val ledgerPostingRequest = purchaseDeliveryAccountingProcessor
            .prepareLedgerRequest(deliveredEvent("20.00", deliveredAtLateUtcEvening))

        assertEquals(LocalDate.of(2026, 3, 11), ledgerPostingRequest.postingDate)
    }

    @Test
    fun `a delivery already in the books is not processed again`() {
        `when`(ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId("DEL-1", locationId))
            .thenReturn(true)

        assertFalse(purchaseDeliveryAccountingProcessor.shouldProcess(deliveredEvent("20.00")))
    }

    @Test
    fun `a delivery not yet in the books is processed`() {
        `when`(ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceLocationId("DEL-1", locationId))
            .thenReturn(false)

        assertTrue(purchaseDeliveryAccountingProcessor.shouldProcess(deliveredEvent("20.00")))
    }

    private fun deliveredEvent(
        deliveryTotal: String,
        deliveredAt: OffsetDateTime = OffsetDateTime.now()
    ) = PurchaseDeliveredEvent(
        eventId = UUID.randomUUID(),
        sourceContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1"),
        timestamp = Instant.now(),
        correlationId = null,
        purchaseId = UUID.randomUUID(),
        purchaseReferenceNumber = "PCH-1",
        deliveryId = UUID.randomUUID(),
        deliveryReferenceNumber = "DEL-1",
        deliveredAt = deliveredAt,
        supplierId = supplierId,
        lines = emptyList(),
        deliveryTotal = Money.of(BigDecimal(deliveryTotal))
    )
}
