package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.RemovedSaleLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

object PendingConsolidationRemovedLines {

    fun remove(saleSession: SaleSession, targetLineKey: UUID): SaleSession {
        val pendingConsolidationBefore = requirePendingConsolidation(saleSession)
        val removedSaleLine = saleSession.saleLines.firstOrNull { it.lineKey == targetLineKey }
            ?: throw RtsGenericException("Line not found on session")
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
            ?: throw RtsGenericException("Removed line not found on session")
        return saleSession.copy(
            saleLines = saleSession.saleLines + restoredLine.saleLine,
            saleAdjustments = saleSession.saleAdjustments + restoredLine.saleAdjustments,
            pendingConsolidation = pendingConsolidationBefore.copy(
                removedSaleLines = pendingConsolidationBefore.removedSaleLines - restoredLine
            ),
        )
    }

    private fun requirePendingConsolidation(saleSession: SaleSession) = saleSession.pendingConsolidation
        ?: throw RtsGenericException("Only a pending consolidation has removed lines")
}
