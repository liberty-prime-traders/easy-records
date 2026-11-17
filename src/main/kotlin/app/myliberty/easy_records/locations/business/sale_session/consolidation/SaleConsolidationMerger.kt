package app.myliberty.easy_records.locations.business.sale_session.consolidation

import app.myliberty.easy_records.locations.business.location_product.api.LocationProductDataFetcher
import app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge.SaleLineMerger
import app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge.SaleLineMergeResult
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraphFacade
import org.springframework.stereotype.Component

@Component
class SaleConsolidationMerger(
    private val locationProductDataFetcher: LocationProductDataFetcher,
    private val unitConversionGraphFacade: UnitConversionGraphFacade,
) {

    fun merge(
        consolidationCopy: ConsolidationCopy,
        reviewedProductLines: ReviewedProductLines,
    ): SaleLineMergeResult {
        val copiedSession = consolidationCopy.saleSession
        val cleanSaleLines = copiedSession.saleLines
            .filter { it.locationProductId !in reviewedProductLines.locationProductIds }
        val cleanSaleLineKeys = cleanSaleLines.map { it.lineKey }.toSet()
        val cleanSaleAdjustments = copiedSession.saleAdjustments.filter { saleAdjustment ->
            saleAdjustment.relatedSaleLineIdentity == null ||
                saleAdjustment.requiredRelatedSaleLineKey() in cleanSaleLineKeys
        }
        val saleLines = cleanSaleLines + reviewedProductLines.saleLines
        return SaleLineMerger.merge(
            saleLines = saleLines,
            saleAdjustments = cleanSaleAdjustments + reviewedProductLines.saleAdjustments,
            inactiveLocationProductIds = locationProductDataFetcher
                .findInactiveIds(saleLines.map { it.locationProductId }.toSet()),
            unitConversionGraph = unitConversionGraphFacade.getOrLoad(),
        )
    }
}
