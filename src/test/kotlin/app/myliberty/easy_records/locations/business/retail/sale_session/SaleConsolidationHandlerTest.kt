package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.configuration.session.LocationSession
import app.myliberty.easy_records.configuration.session.OrgSession
import app.myliberty.easy_records.configuration.session.SessionContext
import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.retail.sale_session.api.ConsolidationSourceSale
import app.myliberty.easy_records.locations.business.retail.sale_session.api.PendingConsolidation
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleConsolidationHandler
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.PendingConsolidationPromoter
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.SaleConsolidationGenerator
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.SaleConsolidationStarter
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.UUID

class SaleConsolidationHandlerTest {

    private val sessionId = UUID.randomUUID()
    private val saleConsolidationGenerator = mock(SaleConsolidationGenerator::class.java)
    private val saleSessionStore = mock(SaleSessionStore::class.java)
    private val pendingConsolidationPromoter = mock(PendingConsolidationPromoter::class.java)
    private val saleConsolidationHandler = SaleConsolidationHandler(
        saleConsolidationStarter = mock(SaleConsolidationStarter::class.java),
        saleConsolidationGenerator = saleConsolidationGenerator,
        saleSessionStore = saleSessionStore,
        saleSessionValidator = SaleSessionValidator(
            mock(AdjustmentReasonService::class.java), mock(LocationProductService::class.java)
        ),
        pendingConsolidationPromoter = pendingConsolidationPromoter,
    )

    @BeforeEach
    fun setUp() {
        SessionContextProvider.setSession(
            SessionContext(
                systemUserId = UUID.randomUUID(),
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1"),
            )
        )
    }

    private fun saleLine() = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = UUID.randomUUID(),
        productLabel = "Sugar",
        productPrintLabel = "Sugar",
        userProvidedQuantity = BigDecimal.ONE,
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal("10.00"),
        defaultSalePrice = BigDecimal("10.00"),
    )

    @Test
    fun `a product that turned inactive is brought onto the form, saved, and generate is refused until it is reviewed`() {
        val heldLine = saleLine()
        val pendingSession: SaleSession = SaleSessionBuilder(sessionId)
            .withHeader(SaleSessionHeader(null, UUID.randomUUID(), null, null, null))
            .withSaleLines(listOf(heldLine))
            .asPendingConsolidation(
                PendingConsolidation(
                    sourceSales = listOf(ConsolidationSourceSale(UUID.randomUUID(), 0L, "SALE01")),
                    sourceSaleReferenceByLineKey = mapOf(heldLine.lineKey to "SALE01"),
                )
            )
            .build()
        val promotedSession = pendingSession.copy(saleLines = pendingSession.saleLines + saleLine())
        `when`(saleSessionStore.load(sessionId)).thenReturn(pendingSession)
        `when`(pendingConsolidationPromoter.promote(pendingSession)).thenReturn(promotedSession)

        val refusal = assertThrows(LibertyGenericException::class.java) { saleConsolidationHandler.generate(sessionId) }

        assertTrue(refusal.message.contains("still cannot be combined"))
        verify(saleSessionStore).save(promotedSession)
        verifyNoInteractions(saleConsolidationGenerator)
    }
}
