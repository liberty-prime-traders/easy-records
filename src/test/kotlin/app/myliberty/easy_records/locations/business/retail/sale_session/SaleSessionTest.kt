package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale_session.api.PendingConsolidation
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionKind
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionTotals
import app.myliberty.easy_records.util.enums.SystemContact
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class SaleSessionTest {

    @Test
    fun `no warning when productsVersion matches productsReservedAtVersion`() {
        val saleSession = buildSaleSession(productsVersion = 3, productsReservedAtVersion = 3)
        assertFalse(saleSession.showUnreservedChangesWarning)
    }

    @Test
    fun `warns when productsVersion is ahead of productsReservedAtVersion`() {
        val saleSession = buildSaleSession(productsVersion = 4, productsReservedAtVersion = 3)
        assertTrue(saleSession.showUnreservedChangesWarning)
    }

    @Test
    fun `a pending consolidation never warns, however far its products version is ahead`() {
        val pendingSession = buildSaleSession(productsVersion = 4, productsReservedAtVersion = 0).copy(
            saleId = null,
            saleVersion = null,
            kind = SaleSessionKind.PENDING_CONSOLIDATION,
            pendingConsolidation = PendingConsolidation(emptyList(), emptyMap()),
        )
        assertFalse(pendingSession.showUnreservedChangesWarning)
    }

    private fun buildSaleSession(productsVersion: Long, productsReservedAtVersion: Long): SaleSession {
        val now = OffsetDateTime.now()
        val userId = UUID.randomUUID()
        return SaleSession(
            sessionId = UUID.randomUUID(),
            locationId = UUID.randomUUID(),
            saleId = UUID.randomUUID(),
            saleVersion = 1,
            productsVersion = productsVersion,
            productsReservedAtVersion = productsReservedAtVersion,
            originalStatus = SaleStatus.DRAFT,
            createdById = userId,
            createdAt = now,
            lastUpdatedAt = now,
            lastAccessedById = userId,
            lastAccessedAt = now,
            header = SaleSessionHeader(
                contactId = SystemContact.WALK_IN.id,
                soldById = null,
                dateSold = null,
                notes = null,
                referenceNumber = null,
            ),
            saleLines = emptyList(),
            saleAdjustments = emptyList(),
            salePayments = emptyList(),
            totals = SaleSessionTotals.ZERO,
        )
    }
}
