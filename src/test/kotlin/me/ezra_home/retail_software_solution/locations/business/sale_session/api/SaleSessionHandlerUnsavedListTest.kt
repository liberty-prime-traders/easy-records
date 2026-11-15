package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionAssembler
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionCopier
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionInMemoryStore
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionLoader
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.OffsetDateTime
import java.util.UUID

class SaleSessionHandlerUnsavedListTest {

    private var sessionsHandedToAssembler: List<SaleSession> = emptyList()
    private val saleSessionStore = SaleSessionInMemoryStore()
    private val saleSessionAssembler = mock(SaleSessionAssembler::class.java) { invocation ->
        sessionsHandedToAssembler = invocation.getArgument(0)
        emptyList<SaleSessionSummaryDto>()
    }
    private val saleSessionHandler = SaleSessionHandler(
        saleSessionStore = saleSessionStore,
        saleSessionLoader = mock(SaleSessionLoader::class.java),
        saleSessionCopier = mock(SaleSessionCopier::class.java),
        saleSessionAssembler = saleSessionAssembler,
        saleSessionUpdateFinalizer = mock(SaleSessionUpdateFinalizer::class.java),
    )

    private fun session(saleId: UUID?, kind: SaleSessionKind): SaleSession = SaleSession(
        sessionId = UUID.randomUUID(),
        locationId = UUID.randomUUID(),
        saleId = saleId,
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
        saleLines = emptyList(),
        saleAdjustments = emptyList(),
        salePayments = emptyList(),
        totals = SaleSessionTotals.ZERO,
        kind = kind,
        pendingConsolidation = if (kind == SaleSessionKind.PENDING_CONSOLIDATION) {
            PendingConsolidation(emptyList(), emptyMap())
        } else {
            null
        },
    )

    @Test
    fun `the unsaved list holds new sales and pending consolidations, not sessions of saved sales`() {
        val newSaleSession = session(null, SaleSessionKind.SALE)
        val pendingSession = session(null, SaleSessionKind.PENDING_CONSOLIDATION)
        val savedSaleSession = session(UUID.randomUUID(), SaleSessionKind.SALE)
        listOf(newSaleSession, pendingSession, savedSaleSession).forEach(saleSessionStore::save)

        saleSessionHandler.getSessionsForUnsavedSales(mineOnly = false)

        assertEquals(
            setOf(newSaleSession.sessionId, pendingSession.sessionId),
            sessionsHandedToAssembler.map { it.sessionId }.toSet()
        )
    }
}
