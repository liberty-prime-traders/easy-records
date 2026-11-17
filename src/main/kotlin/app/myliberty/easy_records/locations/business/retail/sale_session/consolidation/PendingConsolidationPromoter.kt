package app.myliberty.easy_records.locations.business.sale_session.consolidation

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.sale.api.SaleDataFetcher
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionCopier
import app.myliberty.easy_records.locations.business.sale_session.SaleSessionStockOverlay
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionKind
import org.springframework.stereotype.Component

@Component
class PendingConsolidationPromoter(
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val saleDataFetcher: SaleDataFetcher,
    private val saleSessionCopier: SaleSessionCopier,
    private val saleSessionStockOverlay: SaleSessionStockOverlay,
) {

    // A clean product that turns inactive is copied onto the form from the sources, so it is an ordinary held product.
    fun promote(saleSession: SaleSession): SaleSession {
        if (saleSession.kind != SaleSessionKind.PENDING_CONSOLIDATION) return saleSession
        val pendingConsolidation = saleSession.requiredPendingConsolidation()
        val inactiveCleanLocationProductIds = locationProductDataFetcher
            .findInactiveIds(pendingConsolidation.cleanLocationProductIds)
        if (inactiveCleanLocationProductIds.isEmpty()) return saleSession

        val sourceSaleHeaders = saleDataFetcher.getSaleHeaders(pendingConsolidation.sourceSales.map { it.saleId })
        val consolidationCopy = saleSessionCopier
            .copyFromSalesWithSourceReferences(saleSession.sessionId, sourceSaleHeaders)
        val promotedSaleLines = consolidationCopy.saleSession.saleLines
            .filter { it.locationProductId in inactiveCleanLocationProductIds }
        val promotedLineKeys = promotedSaleLines.map { it.lineKey }.toSet()
        val promotedSaleAdjustments = consolidationCopy.saleSession.saleAdjustments
            .filter { it.relatedSaleLineIdentity != null && it.requiredRelatedSaleLineKey() in promotedLineKeys }
        val promotedSourceSaleReferenceByLineKey = promotedLineKeys
            .associateWith { consolidationCopy.sourceSaleReferenceByLineKey.getValue(it) }

        return saleSessionStockOverlay.populate(
            saleSession.copy(
                saleLines = (saleSession.saleLines + promotedSaleLines).sortedBy { it.rank },
                saleAdjustments = saleSession.saleAdjustments + promotedSaleAdjustments,
                pendingConsolidation = pendingConsolidation.copy(
                    sourceSaleReferenceByLineKey =
                        pendingConsolidation.sourceSaleReferenceByLineKey + promotedSourceSaleReferenceByLineKey,
                    cleanLocationProductIds = pendingConsolidation.cleanLocationProductIds -
                        inactiveCleanLocationProductIds,
                ),
            )
        )
    }
}
