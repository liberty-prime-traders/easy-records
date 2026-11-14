package me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_adjustment.api.PriceAdjustmentMethod

data class LineWithAdjustments(
    val saleLine: SaleSessionLine,
    val priceOverride: SaleSessionAdjustment?,
    val otherAdjustments: List<SaleSessionAdjustment>,
) {

    val allAdjustments: List<SaleSessionAdjustment> = listOfNotNull(priceOverride) + otherAdjustments

    companion object {
        fun of(saleLine: SaleSessionLine, adjustments: List<SaleSessionAdjustment>): LineWithAdjustments {
            val (priceOverrides, otherAdjustments) = adjustments.partition {
                it.calculationMethod == PriceAdjustmentMethod.UNIT_PRICE_OVERRIDE
            }
            // PriceOverrideReconciler swaps a line's override for a new one, so a line never holds two.
            check(priceOverrides.size <= 1) { "A sale line carries more than one price override" }
            return LineWithAdjustments(saleLine, priceOverrides.singleOrNull(), otherAdjustments)
        }
    }
}
