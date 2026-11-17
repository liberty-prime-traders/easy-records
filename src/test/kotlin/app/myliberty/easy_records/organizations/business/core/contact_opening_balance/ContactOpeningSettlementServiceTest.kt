package app.myliberty.easy_records.organizations.business.contact_opening_balance

import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.organizations.business.account.api.SystemAccount
import app.myliberty.easy_records.organizations.business.contact.api.ContactDto
import app.myliberty.easy_records.organizations.business.contact.api.ContactService
import app.myliberty.easy_records.organizations.business.contact.api.ContactStatus
import app.myliberty.easy_records.organizations.business.contact.api.ContactType
import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactOpeningSettlementCreateRequest
import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactOpeningSettlementService
import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactOpeningSettlementSummaryResponse
import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactOpeningSettlementVoidRequest
import app.myliberty.easy_records.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingRequest
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerPostingService
import app.myliberty.easy_records.organizations.business.ledger.api.LedgerSourceType
import app.myliberty.easy_records.organizations.business.lock.api.OrgEntityAdvisoryLock
import app.myliberty.easy_records.organizations.business.payment_method.api.PaymentMethodDataFetcher
import app.myliberty.easy_records.util.business.DateTimes
import app.myliberty.easy_records.util.business.MoneyDisplay
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.business.lock.LockNamespaces
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

/** The cap and the lock decide whether a pre-live balance can be over-settled or settled twice by concurrent requests. */
class ContactOpeningSettlementServiceTest {

    private val contactId = UUID.randomUUID()
    private val contactReferenceNumber = "CON000001"
    private val paymentMethodId = UUID.randomUUID()

    private var outstandingAmount = BigDecimal("100.00")
    private val savedSettlementEntities = mutableListOf<ContactOpeningSettlementEntity>()
    private val savedVoidEntities = mutableListOf<ContactOpeningSettlementVoidEntity>()
    private var alreadyVoided = false

    private val storedSettlementEntities = mutableMapOf<UUID, ContactOpeningSettlementEntity>()
    private val contactOpeningSettlementRepository = Proxy.newProxyInstance(
        ContactOpeningSettlementRepository::class.java.classLoader,
        arrayOf(ContactOpeningSettlementRepository::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "saveAndFlush" -> (arguments[0] as ContactOpeningSettlementEntity).also {
                it.referenceNumber = "CSET000001"
                savedSettlementEntities.add(it)
            }
            "findById" -> Optional.ofNullable(storedSettlementEntities[arguments[0] as UUID])
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ContactOpeningSettlementRepository
    private val contactOpeningSettlementVoidRepository = Proxy.newProxyInstance(
        ContactOpeningSettlementVoidRepository::class.java.classLoader,
        arrayOf(ContactOpeningSettlementVoidRepository::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "saveAndFlush" -> (arguments[0] as ContactOpeningSettlementVoidEntity).also { savedVoidEntities.add(it) }
            "existsBySettlementId" -> alreadyVoided
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ContactOpeningSettlementVoidRepository
    private val contactOpeningSettlementFinder = mock(ContactOpeningSettlementFinder::class.java) { outstandingAmount }
    private val contactOpeningSettlementResponseBuilder = mock(ContactOpeningSettlementResponseBuilder::class.java)
    private val contactService = mock(ContactService::class.java)
    private val paymentMethodDataFetcher = mock(PaymentMethodDataFetcher::class.java)
    private val orgEntityAdvisoryLock = mock(OrgEntityAdvisoryLock::class.java)
    private val ledgerPostingService = mock(LedgerPostingService::class.java)
    private val summaryResponse = ContactOpeningSettlementSummaryResponse(
        contactReferenceNumber, MoneyDisplay.of(BigDecimal.ZERO), MoneyDisplay.of(BigDecimal.ZERO), emptyList()
    )

    private val contactOpeningSettlementService = ContactOpeningSettlementService(
        contactOpeningSettlementRepository,
        contactOpeningSettlementVoidRepository,
        contactOpeningSettlementFinder,
        contactOpeningSettlementResponseBuilder,
        contactService,
        paymentMethodDataFetcher,
        orgEntityAdvisoryLock,
        ledgerPostingService
    )

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"))
        )
        givenContact(ContactStatus.ACTIVE)
        `when`(paymentMethodDataFetcher.findAccountCode(paymentMethodId)).thenReturn(SystemAccount.CASH.code)
        `when`(contactOpeningSettlementResponseBuilder.buildSummary(contactReferenceNumber)).thenReturn(summaryResponse)
    }

    @AfterEach
    fun clearSession() {
        SessionContextProvider.clear()
    }

    @Test
    fun `receiving from a contact saves the settlement under the contact lock and posts it once`() {
        contactOpeningSettlementService.recordSettlement(createRequest(ContactSubledgerDirection.RECEIVABLE, "100.00"))

        val savedSettlementEntity = savedSettlementEntities.single()
        assertEquals(ContactSubledgerDirection.RECEIVABLE, savedSettlementEntity.subledgerDirection)
        assertEquals(SystemAccount.CASH.code, savedSettlementEntity.paymentAccountCode)
        assertEquals(listOf(LockNamespaces.CONTACT_OPENING_BALANCE, contactReferenceNumber), lockArguments().single())
        assertEquals(listOf(LedgerSourceType.CONTACT_OPENING_SETTLEMENT), postedRequests().map { it.sourceType })
    }

    @Test
    fun `a settlement above the outstanding opening balance is rejected and nothing is saved or posted`() {
        assertThrows(RtsGenericException::class.java) {
            contactOpeningSettlementService.recordSettlement(createRequest(ContactSubledgerDirection.PAYABLE, "100.01"))
        }

        assertTrue(savedSettlementEntities.isEmpty())
        assertTrue(postedRequests().isEmpty())
    }

    @Test
    fun `an inactive contact cannot be settled`() {
        givenContact(ContactStatus.INACTIVE)

        assertThrows(RtsGenericException::class.java) {
            contactOpeningSettlementService.recordSettlement(createRequest(ContactSubledgerDirection.RECEIVABLE, "10.00"))
        }
        assertTrue(savedSettlementEntities.isEmpty())
    }

    @Test
    fun `a settlement dated in the future is rejected`() {
        val tomorrow = DateTimes.Local.Now.organization().plusDays(1)

        assertThrows(RtsGenericException::class.java) {
            contactOpeningSettlementService.recordSettlement(
                createRequest(ContactSubledgerDirection.RECEIVABLE, "10.00").copy(settlementDate = tomorrow)
            )
        }
        assertTrue(savedSettlementEntities.isEmpty())
    }

    @Test
    fun `voiding saves a void row dated today and posts the reversal under the contact lock`() {
        val settlementEntity = givenStoredSettlement()

        contactOpeningSettlementService.voidSettlement(
            ContactOpeningSettlementVoidRequest(settlementEntity.requiredId(), " mistyped ")
        )

        assertEquals("mistyped", savedVoidEntities.single().reason)
        assertEquals(DateTimes.Local.Now.organization(), savedVoidEntities.single().voidDate)
        assertEquals(listOf(LockNamespaces.CONTACT_OPENING_BALANCE, contactReferenceNumber), lockArguments().single())
        assertEquals(listOf(LedgerSourceType.CONTACT_OPENING_SETTLEMENT_VOID), postedRequests().map { it.sourceType })
        assertTrue(postedRequests().single().reversal)
    }

    @Test
    fun `a settlement cannot be voided twice`() {
        val settlementEntity = givenStoredSettlement()
        alreadyVoided = true

        assertThrows(RtsGenericException::class.java) {
            contactOpeningSettlementService.voidSettlement(
                ContactOpeningSettlementVoidRequest(settlementEntity.requiredId())
            )
        }
        assertTrue(savedVoidEntities.isEmpty())
        assertTrue(postedRequests().isEmpty())
    }

    private fun givenStoredSettlement(): ContactOpeningSettlementEntity {
        val settlementEntity = ContactOpeningSettlementEntity(
            contactReferenceNumber = contactReferenceNumber,
            subledgerDirection = ContactSubledgerDirection.RECEIVABLE,
            paymentAccountCode = SystemAccount.CASH.code,
            amount = BigDecimal("25.00"),
            settlementDate = DateTimes.Local.Now.organization(),
            note = null
        ).also {
            it.id = UUID.randomUUID()
            it.referenceNumber = "CSET000001"
        }
        storedSettlementEntities[settlementEntity.requiredId()] = settlementEntity
        return settlementEntity
    }

    private fun givenContact(contactStatus: ContactStatus) {
        `when`(contactService.getContactById(contactId)).thenReturn(
            ContactDto(
                id = contactId,
                createdById = UUID.randomUUID(),
                createdOn = OffsetDateTime.now(),
                referenceNumber = contactReferenceNumber,
                contactTypes = setOf(ContactType.CUSTOMER),
                firstName = "Test",
                status = contactStatus
            )
        )
    }

    private fun createRequest(subledgerDirection: ContactSubledgerDirection, amount: String) =
        ContactOpeningSettlementCreateRequest(
            contactId = contactId,
            subledgerDirection = subledgerDirection,
            paymentMethodId = paymentMethodId,
            amount = PositiveMoney.of(BigDecimal(amount)),
            settlementDate = DateTimes.Local.Now.organization(),
            note = null
        )

    private fun postedRequests(): List<LedgerPostingRequest> =
        mockingDetails(ledgerPostingService).invocations
            .filter { it.method.name == "post" }
            .map { it.arguments[0] as LedgerPostingRequest }

    private fun lockArguments() =
        mockingDetails(orgEntityAdvisoryLock).invocations
            .filter { it.method.name == "acquire" }
            .map { it.arguments.toList() }
}
