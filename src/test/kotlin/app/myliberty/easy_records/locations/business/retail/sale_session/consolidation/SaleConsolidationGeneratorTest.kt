package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.support.anyNonNull
import java.math.BigDecimal
import app.myliberty.easy_records.locations.business.retail.sale_session.DomainToSessionMapper
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionAssembler
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionBuilder
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionCopier
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionLoader
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionStore
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionUpdateFinalizer
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionValidator
import app.myliberty.easy_records.locations.business.retail.sale_session.SessionToSaveRequestMapper
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod
import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.sale.api.DraftSalePersister
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleReferenceAllocator
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSaveRequest
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleSaveResult
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleUpdater
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentFetcher
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentService
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentSnapshot
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentVoidCreateDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.ConsolidationSourceSale
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionPayment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionResponseDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge.ConsolidationProblem
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge.ConsolidationProblemKind
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge.SaleLineMergeResult
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.OffsetDateTime
import java.util.UUID

@WithDefaultOrganizationSession
class SaleConsolidationGeneratorTest {

    private val saleDataFetcher = mock(SaleDataFetcher::class.java)
    private val saleUpdater = mock(SaleUpdater::class.java)
    private val draftSalePersister = mock(DraftSalePersister::class.java)
    private val saleReferenceAllocator = mock(SaleReferenceAllocator::class.java)
    private val salePaymentFetcher = mock(SalePaymentFetcher::class.java)
    private val salePaymentService = mock(SalePaymentService::class.java)
    private val saleSessionCopier = mock(SaleSessionCopier::class.java)
    private val saleConsolidationMerger = mock(SaleConsolidationMerger::class.java)
    private val saleSessionValidator = mock(SaleSessionValidator::class.java)
    private val saleSessionLoader = mock(SaleSessionLoader::class.java)
    private val saleSessionUpdateFinalizer =
        mock(SaleSessionUpdateFinalizer::class.java) { invocation -> invocation.getArgument<SaleSession>(0) }
    private val saleSessionAssembler = mock(SaleSessionAssembler::class.java)
    private val sessionToSaveRequestMapper = mock(SessionToSaveRequestMapper::class.java)
    private val domainToSessionMapper = mock(DomainToSessionMapper::class.java)
    private val saleSessionStore = mock(SaleSessionStore::class.java)

    private val saleConsolidationGenerator = SaleConsolidationGenerator(
        ConsolidationSources(saleDataFetcher, saleSessionValidator),
        ConsolidationSourcePayments(salePaymentFetcher, salePaymentService, domainToSessionMapper),
        ConsolidationSessionCleanup(saleSessionStore),
        saleUpdater, draftSalePersister, saleReferenceAllocator, saleSessionCopier, saleConsolidationMerger,
        saleSessionValidator, saleSessionLoader, saleSessionUpdateFinalizer, saleSessionAssembler,
        sessionToSaveRequestMapper,
    )

    private val contactId = UUID.randomUUID()
    private val consolidatedReferenceNumber = "SALE99"
    private val newSaleId = UUID.randomUUID()
    private val responseDto = mock(SaleSessionResponseDto::class.java)

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = UUID.randomUUID(),
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1"),
            )
        )
        TransactionSynchronizationManager.initSynchronization()
    }

    @AfterEach
    fun clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization()
    }

    @Test
    fun `sources are locked together, then payments are voided, then sources, then the draft is saved`() {
        val laterId = UUID.fromString("00000000-0000-0000-0000-00000000000b")
        val earlierId = UUID.fromString("00000000-0000-0000-0000-00000000000a")
        val firstSelected = header("SALE01", SaleStatus.CONFIRMED, laterId)
        val secondSelected = header("SALE02", SaleStatus.DRAFT, earlierId)
        val activePayment = payment(voidedReason = null)
        stubHappyPath(listOf(firstSelected, secondSelected))
        stubPayments(firstSelected to listOf(activePayment), secondSelected to emptyList())

        val response = saleConsolidationGenerator.generate(input(firstSelected, secondSelected))

        assertEquals(responseDto, response)
        val inOrder = inOrder(saleDataFetcher, salePaymentService, saleUpdater, draftSalePersister)
        inOrder.verify(saleDataFetcher).lockAndGetSaleHeaders(listOf(firstSelected.id, secondSelected.id))
        inOrder.verify(salePaymentService).voidPayments(
            listOf(SalePaymentVoidCreateDto(activePayment.id, "Consolidated into $consolidatedReferenceNumber"))
        )
        inOrder.verify(saleUpdater)
            .voidSourcesOfConsolidation(listOf(firstSelected.id, secondSelected.id), consolidatedReferenceNumber)
        inOrder.verify(draftSalePersister).saveDraft(expectedSaveRequest())
    }

    @Test
    fun `a source that changed since selection stops everything before any void`() {
        val unchanged = header("SALE01", SaleStatus.DRAFT)
        val changed = header("SALE02", SaleStatus.DRAFT)
        stubHappyPath(listOf(unchanged, changed))
        val staleInput = ConsolidationGenerateInput(
            sourceSales = listOf(
                sourceSaleOf(unchanged), sourceSaleOf(changed).copy(saleVersion = changed.version - 1)
            ),
            reviewedProductLines = ReviewedProductLines.NONE,
            pendingSessionId = null,
        )

        val refusal = assertThrows(LibertyGenericException::class.java) { saleConsolidationGenerator.generate(staleInput) }

        assertTrue(refusal.message.contains("SALE02"))
        verifyNothingWasChanged()
    }

    @Test
    fun `problems that remain stop everything before any void`() {
        val sources = listOf(header("SALE01", SaleStatus.DRAFT), header("SALE02", SaleStatus.DRAFT))
        stubHappyPath(sources)
        val problem = ConsolidationProblem(
            UUID.randomUUID(), ConsolidationProblemKind.entries.first(), listOf(SessionIdentity.mintFreshIdentity())
        )
        `when`(mergeOfTheCopyWithNothingHeld())
            .thenReturn(mergeResult().copy(problems = listOf(problem)))

        assertThrows(LibertyGenericException::class.java) {
            saleConsolidationGenerator.generate(input(*sources.toTypedArray()))
        }

        verifyNothingWasChanged()
    }

    @Test
    fun `redis sessions are replaced only after the commit`() {
        val sources = listOf(header("SALE01", SaleStatus.DRAFT), header("SALE02", SaleStatus.DRAFT))
        stubHappyPath(sources)
        val sourceOpenSession = sessionOf(sources.first().id)
        `when`(saleSessionStore.findOpenSessionForSale(sources.first().id)).thenReturn(sourceOpenSession)
        val pendingSessionId = UUID.randomUUID()

        saleConsolidationGenerator.generate(input(*sources.toTypedArray()).copy(pendingSessionId = pendingSessionId))

        verify(saleSessionStore, never()).save(anyNonNull(SaleSession::class.java))
        verify(saleSessionStore, never()).delete(anyNonNull(UUID::class.java))
        TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
        verify(saleSessionStore).save(newSession())
        verify(saleSessionStore).delete(sourceOpenSession.sessionId)
        verify(saleSessionStore).delete(pendingSessionId)
    }

    @Test
    fun `payments are re-recorded on the new draft with a fresh identity and no payment date`() {
        val sources = listOf(header("SALE01", SaleStatus.CONFIRMED), header("SALE02", SaleStatus.DRAFT))
        stubHappyPath(sources)
        val activePayment = payment(voidedReason = null)
        stubPayments(sources.first() to listOf(activePayment), sources.last() to emptyList())
        var savedSession: SaleSession? = null
        `when`(sessionToSaveRequestMapper.toSaleSaveRequest(anyNonNull(SaleSession::class.java))).thenAnswer {
            savedSession = it.getArgument(0)
            baseSaveRequest()
        }

        saleConsolidationGenerator.generate(input(*sources.toTypedArray()))

        val recordedPayment = savedSession!!.salePayments.single()
        assertNull(recordedPayment.paymentDate)
        assertFalse(recordedPayment.identity.isPersisted())
        assertEquals(activePayment.amount, recordedPayment.amount)
    }

    @Test
    fun `generating from an already merged copy does not lock, copy or merge again`() {
        val sources = listOf(header("SALE01", SaleStatus.CONFIRMED), header("SALE02", SaleStatus.DRAFT))
        stubHappyPath(sources)

        saleConsolidationGenerator.generateFromMerged(
            sources, consolidationCopyInstance, mergeResult(), null, keepOrderLevelAdjustments = true
        )

        verify(saleDataFetcher, never()).lockAndGetSaleHeaders(eqNonNull(sources.map { it.id }))
        verify(saleSessionCopier, never())
            .copyFromSalesWithSourceReferences(anyNonNull(UUID::class.java), eqNonNull(sources))
        verify(saleUpdater).voidSourcesOfConsolidation(sources.map { it.id }, consolidatedReferenceNumber)
    }

    @Test
    fun `sale-wide adjustments are left off the new sale when it comes from a pending consolidation`() {
        val sources = listOf(header("SALE01", SaleStatus.CONFIRMED), header("SALE02", SaleStatus.DRAFT))
        stubHappyPath(sources)
        val sourceDiscount = SaleSessionAdjustment(
            identity = SessionIdentity.mintFreshIdentity(),
            relatedSaleLineIdentity = null,
            adjustmentReasonId = UUID.randomUUID(),
            direction = AdjustmentDirection.DISCOUNT,
            calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
            value = BigDecimal("5"),
            note = null,
            approvedById = null,
        )

        saleConsolidationGenerator.generateFromMerged(
            sources,
            consolidationCopyInstance,
            mergeResult().copy(orderLevelSaleAdjustments = listOf(sourceDiscount)),
            null,
            keepOrderLevelAdjustments = false,
        )

        verify(draftSalePersister).saveDraft(expectedSaveRequest())
    }

    @Test
    fun `a payment that was already voided is not voided again`() {
        val sources = listOf(header("SALE01", SaleStatus.CONFIRMED), header("SALE02", SaleStatus.DRAFT))
        stubHappyPath(sources)
        stubPayments(sources.first() to listOf(payment(voidedReason = "entered twice")), sources.last() to emptyList())

        saleConsolidationGenerator.generate(input(*sources.toTypedArray()))

        verifyNoInteractions(salePaymentService)
    }

    private fun stubPayments(vararg paymentsBySource: Pair<SaleHeaderDto, List<SalePaymentSnapshot>>) {
        `when`(salePaymentFetcher.getPaymentSnapshotsBySaleId(eqNonNull(paymentsBySource.map { it.first.id })))
            .thenReturn(paymentsBySource.associate { it.first.id to it.second })
    }

    private fun mergeOfTheCopyWithNothingHeld() =
        saleConsolidationMerger.merge(eqNonNull(consolidationCopyInstance), eqNonNull(ReviewedProductLines.NONE))

    private fun verifyNothingWasChanged() {
        verifyNoInteractions(saleUpdater, salePaymentService, draftSalePersister, saleReferenceAllocator)
    }

    private fun stubHappyPath(sources: List<SaleHeaderDto>) {
        `when`(saleDataFetcher.lockAndGetSaleHeaders(eqNonNull(sources.map { it.id }))).thenReturn(sources)
        stubPayments(*sources.map { it to emptyList<SalePaymentSnapshot>() }.toTypedArray())
        `when`(saleSessionCopier.copyFromSalesWithSourceReferences(anyNonNull(UUID::class.java), eqNonNull(sources)))
            .thenReturn(consolidationCopyInstance)
        `when`(mergeOfTheCopyWithNothingHeld())
            .thenReturn(mergeResult())
        `when`(saleReferenceAllocator.allocate()).thenReturn(consolidatedReferenceNumber)
        `when`(sessionToSaveRequestMapper.toSaleSaveRequest(anyNonNull(SaleSession::class.java)))
            .thenReturn(baseSaveRequest())
        `when`(draftSalePersister.saveDraft(eqNonNull(expectedSaveRequest()))).thenReturn(saveResult())
        `when`(saleSessionLoader.loadFromSale(anyNonNull(UUID::class.java), eqNonNull(newSaleId)))
            .thenReturn(newSession())
        `when`(saleSessionAssembler.buildResponse(newSession())).thenReturn(responseDto)
        `when`(domainToSessionMapper.toSaleSessionPayment(anyNonNull(SalePaymentSnapshot::class.java)))
            .thenAnswer { invocation ->
                val snapshot = invocation.getArgument<SalePaymentSnapshot>(0)
                SaleSessionPayment(
                    SessionIdentity.persisted(snapshot.id), snapshot.paymentMethodId, snapshot.amount, null,
                    OffsetDateTime.now().minusDays(40)
                )
            }
    }

    private val sessionId = UUID.randomUUID()

    // One instance per test: a session stamps the time it was built, so two builds are never equal.
    private val consolidationCopyInstance by lazy {
        ConsolidationCopy(sessionOf(null, sessionId), emptyMap(), emptyMap(), emptyMap(), emptySet())
    }

    private fun sessionOf(saleId: UUID?, sessionId: UUID = UUID.randomUUID()): SaleSession =
        SaleSessionBuilder(sessionId)
            .withHeader(SaleSessionHeader(null, contactId, null, null, null))
            .build()
            .copy(saleId = saleId)

    private val newSessionInstance: SaleSession by lazy { sessionOf(newSaleId) }

    private fun newSession() = newSessionInstance

    private fun mergeResult() = SaleLineMergeResult(
        mergedSaleLines = emptyList(),
        mergedSaleAdjustments = emptyList(),
        problemSaleLines = emptyList(),
        problemSaleAdjustments = emptyList(),
        orderLevelSaleAdjustments = emptyList(),
        problems = emptyList(),
    )

    private fun baseSaveRequest() = SaleSaveRequest(
        saleId = null, expectedVersion = null, contactId = contactId, soldById = null, dateSold = null, notes = null,
        saleLines = emptyList(), saleAdjustments = emptyList(), salePayments = emptyList(),
    )

    private fun expectedSaveRequest() =
        baseSaveRequest().copy(preAllocatedReferenceNumber = consolidatedReferenceNumber)

    private fun saveResult() = SaleSaveResult(
        saleId = newSaleId, saleReferenceNumber = consolidatedReferenceNumber, newVersion = 0,
        saleStatus = SaleStatus.DRAFT, dateSold = null, soldById = null, saleLineIdsByClientKey = emptyMap(),
        saleAdjustmentIdsByClientKey = emptyMap(), persistedSalePaymentsByClientKey = emptyMap(),
    )

    private fun input(vararg sources: SaleHeaderDto) = ConsolidationGenerateInput(
        sourceSales = sources.map { sourceSaleOf(it) },
        reviewedProductLines = ReviewedProductLines.NONE,
        pendingSessionId = null,
    )

    private fun sourceSaleOf(header: SaleHeaderDto) =
        ConsolidationSourceSale(header.id, header.version, header.referenceNumber)

    private fun header(
        referenceNumber: String,
        status: SaleStatus,
        id: UUID = UUID.randomUUID(),
    ) = SaleHeaderDto(
        id = id, referenceNumber = referenceNumber, version = 3, status = status, contactId = contactId,
        soldById = null, dateSold = null, notes = null,
    )

    private fun payment(voidedReason: String?) = SalePaymentSnapshot(
        id = UUID.randomUUID(), paymentMethodId = UUID.randomUUID(), amount = BigDecimal("10.00"),
        reference = null, paymentDate = null, voidedReason = voidedReason,
    )


    private fun <T> eqNonNull(value: T): T {
        eq(value)
        return value
    }
}
