package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import java.util.UUID

data class ReviewedProductLines(
    val locationProductIds: Set<UUID>,
    val saleLines: List<SaleSessionLine>,
    val saleAdjustments: List<SaleSessionAdjustment>,
) {

    companion object {
        val NONE = ReviewedProductLines(emptySet(), emptyList(), emptyList())

        // A product whose lines were all removed was still reviewed, so its source lines are not recomputed back in.
        fun of(pendingSession: SaleSession): ReviewedProductLines {
            val removedLocationProductIds = pendingSession.requiredPendingConsolidation().removedSaleLines
                .map { it.saleLine.locationProductId }
            return ReviewedProductLines(
                locationProductIds = pendingSession.saleLines.map { it.locationProductId }.toSet() +
                    removedLocationProductIds,
                saleLines = pendingSession.saleLines,
                saleAdjustments = pendingSession.saleAdjustments,
            )
        }
    }
}
