package app.myliberty.easy_records.organizations.business.core.ledger

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.messaging.kafka.common.EventSourceContext
import app.myliberty.easy_records.messaging.kafka.transaction.events.OpeningStockDeclaredEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveredLineDto
import app.myliberty.easy_records.messaging.kafka.transaction.events.PurchaseDeliveryRevisedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.SupplierPaymentRecordedEvent
import app.myliberty.easy_records.messaging.kafka.transaction.events.SupplierPaymentVoidedEvent
import app.myliberty.easy_records.organizations.business.core.account.api.RecordingAccountService
import app.myliberty.easy_records.organizations.business.core.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.core.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.core.account.api.AccountPosting
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.core.ledger.helpers.LedgerPostingGate
import app.myliberty.easy_records.organizations.business.core.ledger.processors.OpeningStockAccountingProcessor
import app.myliberty.easy_records.organizations.business.core.ledger.processors.PurchaseDeliveryAccountingProcessor
import app.myliberty.easy_records.organizations.business.core.ledger.processors.PurchaseDeliveryRevisionAccountingProcessor
import app.myliberty.easy_records.organizations.business.core.ledger.processors.SupplierPaymentAccountingProcessor
import app.myliberty.easy_records.organizations.business.core.ledger.processors.SupplierPaymentVoidAccountingProcessor
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
import java.time.OffsetDateTime
import java.util.UUID
import app.myliberty.easy_records.organizations.business.core.ledger.api.LedgerSourceType
import app.myliberty.easy_records.util.business.PositiveMoney

class SupplierAndOpeningPostingBalanceTest {

    private val recordingAccountService = RecordingAccountService()
    private val supplierId = UUID.randomUUID()
    private val contactService = mock(ContactService::class.java).also { contactService ->
        val supplierDto = mock(ContactDto::class.java)
        `when`(supplierDto.referenceNumber).thenReturn("SUPPLIER-1")
        `when`(contactService.getContactById(supplierId)).thenReturn(supplierDto)
    }
    private val ledgerEntryGroupRepository = mock(LedgerEntryGroupRepository::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)

    private val purchaseDeliveryProcessor = PurchaseDeliveryAccountingProcessor(contactService, ledgerEntryGroupRepository, ledgerPostingService)
    private val purchaseDeliveryRevisionProcessor = PurchaseDeliveryRevisionAccountingProcessor(
        contactService, LedgerPostingGate(ledgerEntryGroupRepository), ledgerPostingService
    )
    private val supplierPaymentProcessor = SupplierPaymentAccountingProcessor(contactService, ledgerEntryGroupRepository, ledgerPostingService)
    private val supplierPaymentVoidProcessor = SupplierPaymentVoidAccountingProcessor(contactService, LedgerPostingGate(ledgerEntryGroupRepository), ledgerPostingService)
    private val openingStockProcessor = OpeningStockAccountingProcessor(ledgerEntryGroupRepository, ledgerPostingService)

    private val locationLevelContext = EventSourceContext.LocationLevel(orgSchema = "org-a", locationSchema = "loc-1")
    private val locationId = UUID.randomUUID()

    @BeforeEach
    fun setSession() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = locationId, schemaName = "loc-1")
            )
        )
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `a delivery raises inventory and trade payables`() {
        post(purchaseDeliveryProcessor.prepareLedgerRequest(purchaseDeliveredEvent()))

        assertBalance("10.00", SystemAccount.INVENTORY)
        assertBalance("10.00", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `a revision that raises a line total raises inventory and trade payables by the change`() {
        post(purchaseDeliveryProcessor.prepareLedgerRequest(purchaseDeliveredEvent()))
        post(purchaseDeliveryRevisionProcessor.prepareLedgerRequest(purchaseDeliveryRevisedEvent("10.00", "14.50")))

        assertBalance("14.50", SystemAccount.INVENTORY)
        assertBalance("14.50", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `a revision that lowers a line total lowers inventory and trade payables by the change`() {
        post(purchaseDeliveryProcessor.prepareLedgerRequest(purchaseDeliveredEvent()))
        post(purchaseDeliveryRevisionProcessor.prepareLedgerRequest(purchaseDeliveryRevisedEvent("10.00", "3.25")))

        assertBalance("3.25", SystemAccount.INVENTORY)
        assertBalance("3.25", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `paying a supplier lowers cash and trade payables`() {
        post(purchaseDeliveryProcessor.prepareLedgerRequest(purchaseDeliveredEvent()))
        post(supplierPaymentProcessor.prepareLedgerRequest(supplierPaymentRecordedEvent("4.00")))

        assertBalance("-4.00", SystemAccount.CASH)
        assertBalance("6.00", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `voiding a supplier payment restores cash and trade payables`() {
        post(purchaseDeliveryProcessor.prepareLedgerRequest(purchaseDeliveredEvent()))
        post(supplierPaymentProcessor.prepareLedgerRequest(supplierPaymentRecordedEvent("4.00")))
        `when`(
            ledgerEntryGroupRepository.existsBySourceReferenceNumberAndSourceTypeAndSourceLocationId(
                "SPAY-1", LedgerSourceType.SUPPLIER_PAYMENT, locationId
            )
        ).thenReturn(true)
        post(supplierPaymentVoidProcessor.prepareLedgerRequest(supplierPaymentVoidedEvent("4.00")))

        assertBalance("0.00", SystemAccount.CASH)
        assertBalance("10.00", SystemAccount.TRADE_PAYABLES)
    }

    @Test
    fun `opening stock raises inventory and opening balance equity`() {
        post(openingStockProcessor.prepareLedgerRequest(openingStockDeclaredEvent()))

        assertBalance("6.00", SystemAccount.INVENTORY)
        assertBalance("6.00", SystemAccount.OPENING_BALANCE_EQUITY)
    }

    private fun post(ledgerPostingRequest: LedgerPostingRequest) {
        LedgerEntriesValidator.validate(ledgerPostingRequest.entries)
        val accountPostings = ledgerPostingRequest.entries.map {
            AccountPosting(accountCode = it.accountCode, amount = it.amount.toBigDecimal(), entryType = it.entryType)
        }
        recordingAccountService.accountService.assertPostable(accountPostings)
        recordingAccountService.accountService.patchBalances(accountPostings)
    }

    private fun assertBalance(expected: String, systemAccount: SystemAccount) {
        assertEquals(
            0,
            BigDecimal(expected).compareTo(recordingAccountService.balance(systemAccount)),
            "${systemAccount.name} expected $expected but was ${recordingAccountService.balance(systemAccount)}"
        )
    }

    private fun purchaseDeliveryRevisedEvent(revisedLineTotal: String, replacementLineTotal: String) =
        PurchaseDeliveryRevisedEvent(
            eventId = UUID.randomUUID(),
            sourceContext = locationLevelContext,
            timestamp = Instant.now(),
            correlationId = null,
            stockRevisionId = UUID.randomUUID(),
            stockRevisionReferenceNumber = "STRV000001",
            purchaseReferenceNumber = "PCH-1",
            supplierId = supplierId,
            revisedLineTotal = Money.of(BigDecimal(revisedLineTotal)),
            replacementLineTotal = Money.of(BigDecimal(replacementLineTotal)),
            postingDate = LocalDate.now()
        )

    private fun purchaseDeliveredEvent() = PurchaseDeliveredEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        purchaseId = UUID.randomUUID(),
        purchaseReferenceNumber = "PCH-1",
        deliveryId = UUID.randomUUID(),
        deliveryReferenceNumber = "DEL-1",
        deliveredAt = OffsetDateTime.now(),
        supplierId = supplierId,
        lines = listOf(
            PurchaseDeliveredLineDto(
                deliveryLineId = UUID.randomUUID(),
                lineReferenceNumber = "DEL-1-1",
                locationProductId = UUID.randomUUID(),
                userProvidedQuantityDelivered = BigDecimal("2"),
                unitId = UUID.randomUUID(),
                storageUnitsPerUserProvidedUnit = 1L,
                unitCost = BigDecimal("5.00")
            )
        ),
        deliveryTotal = Money.of(BigDecimal("10.00"))
    )

    private fun supplierPaymentRecordedEvent(amount: String) = SupplierPaymentRecordedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        paymentId = UUID.randomUUID(),
        supplierId = supplierId,
        paymentMethodAccountCode = SystemAccount.CASH.code,
        amount = PositiveMoney.of(BigDecimal(amount)),
        paymentDate = OffsetDateTime.now(),
        paymentReferenceNumber = "SPAY-1"
    )

    private fun supplierPaymentVoidedEvent(amount: String) = SupplierPaymentVoidedEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        voidId = UUID.randomUUID(),
        paymentId = UUID.randomUUID(),
        supplierId = supplierId,
        paymentMethodAccountCode = SystemAccount.CASH.code,
        amount = PositiveMoney.of(BigDecimal(amount)),
        voidedOn = LocalDate.of(2026, 1, 20),
        paymentReferenceNumber = "SPAY-1"
    )

    private fun openingStockDeclaredEvent() = OpeningStockDeclaredEvent(
        eventId = UUID.randomUUID(),
        sourceContext = locationLevelContext,
        timestamp = Instant.now(),
        correlationId = null,
        openingStockId = UUID.randomUUID(),
        ledgerSourceReferenceNumber = "OS-1",
        locationProductId = UUID.randomUUID(),
        totalCost = Money.of(BigDecimal("6.00")),
        postingDate = LocalDate.of(2026, 1, 1)
    )
}
