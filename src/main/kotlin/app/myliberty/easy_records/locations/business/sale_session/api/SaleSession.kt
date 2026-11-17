package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.SystemContact
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.time.OffsetDateTime
import java.util.UUID

data class SaleSession(
    val sessionId: UUID,
    val locationId: UUID,
    val saleId: UUID?,
    val saleVersion: Long?,
    val productsVersion: Long,
    val productsReservedAtVersion: Long,
    val originalStatus: SaleStatus,
    val createdById: UUID,
    val createdAt: OffsetDateTime,
    val lastUpdatedAt: OffsetDateTime,
    val lastAccessedById: UUID,
    val lastAccessedAt: OffsetDateTime,
    val header: SaleSessionHeader,
    val saleLines: List<SaleSessionLine>,
    val saleAdjustments: List<SaleSessionAdjustment>,
    val salePayments: List<SaleSessionPayment>,
    val totals: SaleSessionTotals,
    val kind: SaleSessionKind = SaleSessionKind.SALE,
    val pendingConsolidation: PendingConsolidation? = null,
) {

    init {
        val isPendingConsolidation = kind == SaleSessionKind.PENDING_CONSOLIDATION
        if (isPendingConsolidation != (pendingConsolidation != null)) {
            throw RtsGenericException(
                "SaleSession $sessionId of kind $kind has inconsistent pending consolidation data"
            )
        }
        if (isPendingConsolidation && (saleId != null || saleVersion != null || salePayments.isNotEmpty())) {
            throw RtsGenericException(
                "SaleSession $sessionId is a pending consolidation, so it has no sale id, sale version or payments"
            )
        }
    }

    val showUnreservedChangesWarning: Boolean =
        SaleSessionRules.reservesStock(kind) && productsReservedAtVersion < productsVersion

    fun mutable(): Boolean =
        originalStatus == SaleStatus.DRAFT

    fun requiredPendingConsolidation(): PendingConsolidation = pendingConsolidation
        ?: throw RtsGenericException("SaleSession $sessionId is not a pending consolidation")

    fun requiredSaleId(): UUID = saleId
        ?: throw RtsGenericException("SaleSession $sessionId with status $originalStatus is missing its saleId")

    fun totalPaid() =
        salePayments.filter { it.voidedReason == null }.sumOf { it.amount }

    fun allows(action: SaleSessionAction): Boolean =
        SaleSessionRules.allows(kind, action) && (!action.needsMutableSale() || mutable())

    fun walkInIsNotFullyCovered(): Boolean =
        header.contactId == SystemContact.WALK_IN.id && totals.paymentTotal < totals.receivableTotal

    fun paymentsExceedTotal(): Boolean =
        totals.receivableTotal.signum() > 0 && totals.paymentTotal > totals.receivableTotal

    fun canAddPayments(): Boolean {
        if (!SaleSessionRules.allows(kind, SaleSessionAction.ADD_PAYMENT)) return false
        if (totals.paymentTotal >= totals.receivableTotal) return false
        return originalStatus == SaleStatus.DRAFT || originalStatus == SaleStatus.CONFIRMED
    }

    fun withoutLine(targetLineKey: UUID): SaleSession {
        if (saleLines.none { it.lineKey == targetLineKey }) throw RtsGenericException("Line not found on session")
        return copy(
            saleLines = saleLines.filter { it.lineKey != targetLineKey },
            saleAdjustments = saleAdjustments.filter { it.relatedSaleLineIdentity?.key() != targetLineKey },
        )
    }

    fun markTouched(userId: UUID): SaleSession {
        val now = DateTimes.Offset.Now.organization()
        return copy(lastUpdatedAt = now, lastAccessedById = userId, lastAccessedAt = now)
    }

    fun markVisited(userId: UUID): SaleSession =
        copy(lastAccessedById = userId, lastAccessedAt = DateTimes.Offset.Now.organization())

}
