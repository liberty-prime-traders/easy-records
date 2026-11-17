package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductService
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.SaleAdjustmentFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentReasonService
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.UUID

@WithDefaultOrganizationSession
class SaleSessionCopierTest {

    private val saleDataFetcher = mock(SaleDataFetcher::class.java)
    private val saleAdjustmentFetcher = mock(SaleAdjustmentFetcher::class.java)
    private val saleSessionStockOverlay =
        mock(SaleSessionStockOverlay::class.java) { invocation -> invocation.getArgument<SaleSession>(0) }
    private val saleSessionValidator = SaleSessionValidator(
        mock(AdjustmentReasonService::class.java),
        mock(LocationProductService::class.java),
    )
    private val saleSessionCopier = SaleSessionCopier(
        saleDataFetcher,
        saleAdjustmentFetcher,
        mock(DomainToSessionMapper::class.java),
        saleSessionStockOverlay,
        saleSessionValidator,
    )

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
    fun `consolidated notes carry one reference-prefixed line per source that has notes and skip the rest`() {
        val contactId = UUID.randomUUID()
        val sourceSaleHeaders = listOf(
            saleHeader("SALE01", contactId, "deliver Monday"),
            saleHeader("SALE02", contactId, null),
            saleHeader("SALE03", contactId, "   "),
            saleHeader("SALE04", contactId, "fragile"),
        )
        stubEmptyContent(sourceSaleHeaders)

        val consolidatedSession = saleSessionCopier
            .copyFromSalesWithSourceReferences(UUID.randomUUID(), sourceSaleHeaders).saleSession

        assertEquals("SALE01 - deliver Monday\nSALE04 - fragile", consolidatedSession.header.notes)
        assertEquals(contactId, consolidatedSession.header.contactId)
        verify(saleDataFetcher, times(1)).getSaleLinesBySaleId(sourceSaleHeaders.map { it.id })
        verify(saleAdjustmentFetcher, times(1)).getAdjustmentsBySaleId(sourceSaleHeaders.map { it.id })
        verify(saleDataFetcher, never()).getSaleLines(sourceSaleHeaders.first().id)
    }

    @Test
    fun `sources without any notes leave the consolidated notes null`() {
        val contactId = UUID.randomUUID()
        val sourceSaleHeaders = listOf(saleHeader("SALE01", contactId, null), saleHeader("SALE02", contactId, null))
        stubEmptyContent(sourceSaleHeaders)

        val consolidatedSession = saleSessionCopier
            .copyFromSalesWithSourceReferences(UUID.randomUUID(), sourceSaleHeaders).saleSession

        assertNull(consolidatedSession.header.notes)
    }

    @Test
    fun `a single sale is refused before anything is fetched`() {
        val sourceSaleHeaders = listOf(saleHeader("SALE01", UUID.randomUUID(), null))

        assertThrows(RtsGenericException::class.java) {
            saleSessionCopier.copyFromSalesWithSourceReferences(UUID.randomUUID(), sourceSaleHeaders)
        }
        verify(saleDataFetcher, never()).getSaleLinesBySaleId(sourceSaleHeaders.map { it.id })
    }

    @Test
    fun `sources of different contacts abort the consolidation`() {
        val sourceSaleHeaders = listOf(
            saleHeader("SALE01", UUID.randomUUID(), null),
            saleHeader("SALE02", UUID.randomUUID(), null),
        )

        assertThrows(RtsGenericException::class.java) {
            saleSessionCopier.copyFromSalesWithSourceReferences(UUID.randomUUID(), sourceSaleHeaders)
        }
    }

    private fun stubEmptyContent(sourceSaleHeaders: List<SaleHeaderDto>) {
        val sourceSaleIds = sourceSaleHeaders.map { it.id }
        `when`(saleDataFetcher.getSaleLinesBySaleId(sourceSaleIds))
            .thenReturn(sourceSaleIds.associateWith { emptyList() })
        `when`(saleAdjustmentFetcher.getAdjustmentsBySaleId(sourceSaleIds))
            .thenReturn(sourceSaleIds.associateWith { emptyList() })
    }

    private fun saleHeader(referenceNumber: String, contactId: UUID, notes: String?) = SaleHeaderDto(
        id = UUID.randomUUID(),
        referenceNumber = referenceNumber,
        version = 0,
        status = SaleStatus.DRAFT,
        contactId = contactId,
        soldById = null,
        dateSold = null,
        notes = notes,
    )
}
