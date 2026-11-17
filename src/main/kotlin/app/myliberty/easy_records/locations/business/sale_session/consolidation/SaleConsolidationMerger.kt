package me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation

import me.ezra_home.retail_software_solution.locations.business.location_product.api.LocationProductDataFetcher
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.line_merge.SaleLineMerger
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.line_merge.SaleLineMergeResult
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraphFacade
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
