package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.configuration.session.SessionContextProvider
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale_session.api.PendingConsolidation
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionHeader
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionKind
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionPayment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionTotals
import app.myliberty.easy_records.util.business.DateTimes
import java.util.UUID

class SaleSessionBuilder(private val sessionId: UUID) {

    private val now = DateTimes.Offset.Now.organization()
    private val userId = SessionContextProvider.getUserId()
    private val locationId = SessionContextProvider.getLocationId()

    private var saleId: UUID? = null
    private var saleVersion: Long? = null
    private var originalStatus = SaleStatus.DRAFT
    private var productsVersion = 0L
    private var header: SaleSessionHeader? = null
    private var saleLines: List<SaleSessionLine> = emptyList()
    private var saleAdjustments: List<SaleSessionAdjustment> = emptyList()
    private var salePayments: List<SaleSessionPayment> = emptyList()
    private var pendingConsolidation: PendingConsolidation? = null

    fun linkedToPersistedSale(saleId: UUID, saleVersion: Long?, originalStatus: SaleStatus): SaleSessionBuilder = apply {
        this.saleId = saleId
        this.saleVersion = saleVersion
        this.originalStatus = originalStatus
    }

    fun asPendingConsolidation(pendingConsolidation: PendingConsolidation): SaleSessionBuilder =
        apply { this.pendingConsolidation = pendingConsolidation }

    fun withHeader(header: SaleSessionHeader): SaleSessionBuilder = apply { this.header = header }

    fun withSaleLines(saleLines: List<SaleSessionLine>): SaleSessionBuilder = apply { this.saleLines = saleLines }

    fun withSaleAdjustments(saleAdjustments: List<SaleSessionAdjustment>): SaleSessionBuilder =
        apply { this.saleAdjustments = saleAdjustments }

    fun withSalePayments(salePayments: List<SaleSessionPayment>): SaleSessionBuilder =
        apply { this.salePayments = salePayments }

    fun withLinesAwaitingReservation(): SaleSessionBuilder = apply { this.productsVersion = 1L }

    fun build(): SaleSession = SaleSession(
        sessionId = sessionId,
        locationId = locationId,
        saleId = saleId,
        saleVersion = saleVersion,
        productsVersion = productsVersion,
        productsReservedAtVersion = 0L,
        originalStatus = originalStatus,
        createdById = userId,
        createdAt = now,
        lastUpdatedAt = now,
        lastAccessedById = userId,
        lastAccessedAt = now,
        header = header ?: throw IllegalStateException("A sale session needs a header before it is built"),
        saleLines = saleLines,
        saleAdjustments = saleAdjustments,
        salePayments = salePayments,
        totals = SaleSessionTotals.ZERO,
        kind = if (pendingConsolidation == null) SaleSessionKind.SALE else SaleSessionKind.PENDING_CONSOLIDATION,
        pendingConsolidation = pendingConsolidation,
    )
}
