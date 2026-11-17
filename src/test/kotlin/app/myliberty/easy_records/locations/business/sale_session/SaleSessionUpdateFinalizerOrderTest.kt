package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionHeader
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionTotals
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.support.WithDefaultOrganizationSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@WithDefaultOrganizationSession
class SaleSessionUpdateFinalizerOrderTest {

    private val saleSessionStore = SaleSessionInMemoryStore()
    private val saleSessionUpdateFinalizer = SaleSessionUpdateFinalizer(
        saleSessionStore, mock(SaleSessionAssembler::class.java), mock(SaleSessionValidator::class.java)
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
    fun `the saved session keeps its lines in rank order, a line added on top comes first`() {
        val riceLine = line("Rice", "1")
        val sugarLine = line("Sugar", "2")
        val addedLine = line("Beans", "-1")
        val session = session(listOf(riceLine, sugarLine, addedLine))

        saleSessionUpdateFinalizer.finalize(session)

        assertEquals(
            listOf("Beans", "Rice", "Sugar"),
            saleSessionStore.load(session.sessionId).saleLines.map { it.productLabel }
        )
    }

    @Test
    fun `the saved session carries totals computed from its lines even when it arrives with none`() {
        val session = session(listOf(line("Rice", "1"), line("Sugar", "2")))

        saleSessionUpdateFinalizer.finalize(session)

        val savedTotals = saleSessionStore.load(session.sessionId).totals
        assertEquals(0, BigDecimal("20.00").compareTo(savedTotals.subtotal))
        assertEquals(0, BigDecimal("20.00").compareTo(savedTotals.receivableTotal))
    }

    private fun line(productLabel: String, rank: String) = SaleSessionLine(
        identity = SessionIdentity.mintFreshIdentity(),
        locationProductId = UUID.randomUUID(),
        productLabel = productLabel,
        productPrintLabel = productLabel,
        userProvidedQuantity = BigDecimal.ONE,
        unitId = UUID.randomUUID(),
        baseUnitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = 1L,
        lineTotal = BigDecimal("10.00"),
        defaultSalePrice = BigDecimal("10.00"),
        rank = BigDecimal(rank),
    )

    private fun session(saleLines: List<SaleSessionLine>) = SaleSession(
        sessionId = UUID.randomUUID(),
        locationId = UUID.randomUUID(),
        saleId = null,
        saleVersion = null,
        productsVersion = 0L,
        productsReservedAtVersion = 0L,
        originalStatus = SaleStatus.DRAFT,
        createdById = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        lastUpdatedAt = OffsetDateTime.now(),
        lastAccessedById = UUID.randomUUID(),
        lastAccessedAt = OffsetDateTime.now(),
        header = SaleSessionHeader(
            referenceNumber = null, contactId = UUID.randomUUID(), soldById = null, dateSold = null, notes = null
        ),
        saleLines = saleLines,
        saleAdjustments = emptyList(),
        salePayments = emptyList(),
        totals = SaleSessionTotals.ZERO,
    )
}
