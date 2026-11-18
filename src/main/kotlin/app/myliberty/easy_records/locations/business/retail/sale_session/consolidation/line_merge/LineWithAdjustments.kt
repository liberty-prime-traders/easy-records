package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod

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
