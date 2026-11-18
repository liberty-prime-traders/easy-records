package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.locations.business.retail.sale_session.api.RemovedSaleLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.util.UUID

object PendingConsolidationRemovedLines {

    fun remove(saleSession: SaleSession, targetLineKey: UUID): SaleSession {
        val pendingConsolidationBefore = requirePendingConsolidation(saleSession)
        val removedSaleLine = saleSession.saleLines.firstOrNull { it.lineKey == targetLineKey }
            ?: throw LibertyGenericException("Line not found on session")
        val removedSaleAdjustments = saleSession.saleAdjustments.filter {
            it.relatedSaleLineIdentity?.key() == targetLineKey
        }
        return saleSession.withoutLine(targetLineKey).copy(
            pendingConsolidation = pendingConsolidationBefore.copy(
                removedSaleLines = pendingConsolidationBefore.removedSaleLines +
                    RemovedSaleLine(removedSaleLine, removedSaleAdjustments)
            )
        )
    }

    fun restore(saleSession: SaleSession, targetLineKey: UUID): SaleSession {
        val pendingConsolidationBefore = requirePendingConsolidation(saleSession)
        val restoredLine = pendingConsolidationBefore.removedSaleLines
            .firstOrNull { it.saleLine.lineKey == targetLineKey }
            ?: throw LibertyGenericException("Removed line not found on session")
        return saleSession.copy(
            saleLines = saleSession.saleLines + restoredLine.saleLine,
            saleAdjustments = saleSession.saleAdjustments + restoredLine.saleAdjustments,
            pendingConsolidation = pendingConsolidationBefore.copy(
                removedSaleLines = pendingConsolidationBefore.removedSaleLines - restoredLine
            ),
        )
    }

    private fun requirePendingConsolidation(saleSession: SaleSession) = saleSession.pendingConsolidation
        ?: throw LibertyGenericException("Only a pending consolidation has removed lines")
}
