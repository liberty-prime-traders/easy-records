package app.myliberty.easy_records.locations.business.sale_session.consolidation

import java.math.BigDecimal
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionBuilder
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionCopier
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionStockOverlay
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionUpdateFinalizer
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionValidator
import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.locations.business.sale_session.api.SessionIdentity
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.sale_adjustment.api.PriceAdjustmentMethod
import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.sale.api.SaleLineDto
import app.myliberty.easy_records.locations.business.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionKind
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionResponseDto
import app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge.ConsolidationProblem
import app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge.ConsolidationProblemKind
import app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge.SaleLineMergeResult
import app.myliberty.easy_records.locations.business.location_product.api.LocationProductService
import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.support.WithDefaultOrganizationSession
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.util.UUID

@WithDefaultOrganizationSession
class SaleConsolidationStarterTest {

    private val saleDataFetcher = mock(SaleDataFetcher::class.java)
    private val saleSessionCopier = mock(SaleSessionCopier::class.java)
    private val saleConsolidationMerger = mock(SaleConsolidationMerger::class.java)
    private val saleConsolidationGenerator = mock(SaleConsolidationGenerator::class.java)
    private val saleSessionStockOverlay =
        mock(SaleSessionStockOverlay::class.java) { invocation -> invocation.getArgument<SaleSession>(0) }
    private val saleSessionUpdateFinalizer = mock(SaleSessionUpdateFinalizer::class.java)
    private val saleConsolidationStarter = SaleConsolidationStarter(
        ConsolidationSources(
            saleDataFetcher,
            SaleSessionValidator(mock(AdjustmentReasonService::class.java), mock(LocationProductService::class.java)),
        ),
        saleSessionCopier,
        saleConsolidationMerger,
        saleConsolidationGenerator,
        saleSessionStockOverlay,
        saleSessionUpdateFinalizer,
    )

    private val contactId = UUID.randomUUID()
    private val responseDto = mock(SaleSessionResponseDto::class.java)

    @BeforeEach
    fun setUserAndLocationSession() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = UUID.randomUUID(),
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1"),
            )
        )
    }

    @Test
    fun `with no problems the merged sales are generated at once without a second copy or merge`() {
        val sources = listOf(header("SALE01", SaleStatus.DRAFT), header("SALE02", SaleStatus.CONFIRMED))
        val mergeResult = mergeResult()
        val consolidationCopy = stubCopyAndMerge(sources, mergeResult)
        `when`(
            saleConsolidationGenerator.generateFromMerged(
                eqNonNull(sources), eqNonNull(consolidationCopy), eqNonNull(mergeResult), isNull(), eqNonNull(true)
            )
        ).thenReturn(responseDto)

        val response = saleConsolidationStarter.start(sources.map { it.id })

        assertEquals(responseDto, response)
        verifyNoInteractions(saleSessionUpdateFinalizer)
        val locksBeforeCopy = inOrder(saleDataFetcher, saleSessionCopier)
        locksBeforeCopy.verify(saleDataFetcher).lockAndGetSaleHeaders(sources.map { it.id })
        locksBeforeCopy.verify(saleSessionCopier)
            .copyFromSalesWithSourceReferences(anyNonNull(UUID::class.java), eqNonNull(sources))
    }

    @Test
    fun `with problems a pending session holding only the problem lines is opened and nothing is generated`() {
        val sources = listOf(header("SALE01", SaleStatus.DRAFT), header("SALE02", SaleStatus.DRAFT))
        val problem = ConsolidationProblem(
            UUID.randomUUID(), ConsolidationProblemKind.entries.first(), emptyList()
        )
        stubCopyAndMerge(sources, mergeResult().copy(problems = listOf(problem)))
        var openedSession: SaleSession? = null
        `when`(saleSessionUpdateFinalizer.finalize(anyNonNull(SaleSession::class.java))).thenAnswer { invocation ->
            openedSession = invocation.getArgument(0)
            responseDto
        }

        val response = saleConsolidationStarter.start(sources.map { it.id })

        assertEquals(responseDto, response)
        verifyNoInteractions(saleConsolidationGenerator)
        assertEquals(SaleSessionKind.PENDING_CONSOLIDATION, openedSession!!.kind)
        val recordedSourceSales = openedSession!!.requiredPendingConsolidation().sourceSales
        assertEquals(sources.map { it.id }, recordedSourceSales.map { it.saleId })
        assertEquals(sources.map { it.version }, recordedSourceSales.map { it.saleVersion })
    }

    @Test
    fun `sale-wide adjustments that cannot be added exactly open a pending session that notes the sales with them`() {
        val sources = listOf(header("SALE01", SaleStatus.DRAFT), header("SALE02", SaleStatus.DRAFT))
        val cleanLine = SaleSessionLine(
            identity = SessionIdentity.mintFreshIdentity(),
            locationProductId = UUID.randomUUID(),
            productLabel = "Sugar",
            productPrintLabel = "Sugar",
            userProvidedQuantity = BigDecimal.ONE,
            unitId = UUID.randomUUID(),
            baseUnitId = UUID.randomUUID(),
            storageUnitsPerUserProvidedUnit = 1L,
            lineTotal = BigDecimal("3.00"),
            defaultSalePrice = BigDecimal("3.00"),
        )
        val sourceDiscount = SaleSessionAdjustment(
            identity = SessionIdentity.mintFreshIdentity(),
            relatedSaleLineIdentity = null,
            adjustmentReasonId = UUID.randomUUID(),
            direction = AdjustmentDirection.DISCOUNT,
            calculationMethod = PriceAdjustmentMethod.PERCENTAGE,
            value = BigDecimal("1"),
            note = null,
            approvedById = null,
        )
        stubCopyAndMerge(
            sources,
            mergeResult().copy(mergedSaleLines = listOf(cleanLine), orderLevelSaleAdjustments = listOf(sourceDiscount)),
            orderLevelAmountByAdjustmentKey = mapOf(sourceDiscount.identity.key() to BigDecimal("1.00")),
            saleReferencesWithOrderLevelAdjustments = setOf("SALE01"),
        )
        var openedSession: SaleSession? = null
        `when`(saleSessionUpdateFinalizer.finalize(anyNonNull(SaleSession::class.java))).thenAnswer { invocation ->
            openedSession = invocation.getArgument(0)
            responseDto
        }

        saleConsolidationStarter.start(sources.map { it.id })

        verifyNoInteractions(saleConsolidationGenerator)
        val pendingConsolidation = openedSession!!.requiredPendingConsolidation()
        assertEquals(setOf("SALE01"), pendingConsolidation.saleReferencesWithOrderLevelAdjustments)
        assertEquals(setOf(cleanLine.locationProductId), pendingConsolidation.cleanLocationProductIds)
    }

    @Test
    fun `a voided sale or a walk-in sale is refused before anything is copied`() {
        val voided = header("SALE01", SaleStatus.VOIDED)
        val draft = header("SALE02", SaleStatus.DRAFT)
        `when`(saleDataFetcher.lockAndGetSaleHeaders(eqNonNull(listOf(voided.id, draft.id))))
            .thenReturn(listOf(voided, draft))

        assertThrows(RtsGenericException::class.java) { saleConsolidationStarter.start(listOf(voided.id, draft.id)) }

        verifyNoInteractions(saleSessionCopier, saleConsolidationGenerator)
    }

    private fun stubCopyAndMerge(
        sources: List<SaleHeaderDto>,
        mergeResult: SaleLineMergeResult,
        orderLevelAmountByAdjustmentKey: Map<UUID, BigDecimal> = emptyMap(),
        saleReferencesWithOrderLevelAdjustments: Set<String> = emptySet(),
    ): ConsolidationCopy {
        `when`(saleDataFetcher.lockAndGetSaleHeaders(eqNonNull(sources.map { it.id }))).thenReturn(sources)
        val consolidationCopy = ConsolidationCopy(
            SaleSessionBuilder(UUID.randomUUID())
                .withHeader(SaleSessionHeader(null, contactId, null, null, null))
                .build(),
            emptyMap(),
            sources.associate { it.id to emptyList<SaleLineDto>() },
            orderLevelAmountByAdjustmentKey,
            saleReferencesWithOrderLevelAdjustments,
        )
        `when`(saleSessionCopier.copyFromSalesWithSourceReferences(anyNonNull(UUID::class.java), eqNonNull(sources)))
            .thenReturn(consolidationCopy)
        `when`(saleConsolidationMerger.merge(eqNonNull(consolidationCopy), eqNonNull(ReviewedProductLines.NONE)))
            .thenReturn(mergeResult)
        return consolidationCopy
    }

    private fun mergeResult() = SaleLineMergeResult(
        mergedSaleLines = emptyList(),
        mergedSaleAdjustments = emptyList(),
        problemSaleLines = emptyList(),
        problemSaleAdjustments = emptyList(),
        orderLevelSaleAdjustments = emptyList(),
        problems = emptyList(),
    )

    private fun header(referenceNumber: String, status: SaleStatus) = SaleHeaderDto(
        id = UUID.randomUUID(), referenceNumber = referenceNumber, version = 4, status = status,
        contactId = contactId, soldById = null, dateSold = null, notes = null,
    )

    // Mockito's any() returns null, which Kotlin rejects for non-null parameters.
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyNonNull(type: Class<T>): T {
        any(type)
        return null as T
    }

    private fun <T> eqNonNull(value: T): T {
        eq(value)
        return value
    }
}
