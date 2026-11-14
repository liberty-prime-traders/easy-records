package me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionTotalsCalculator
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import java.math.BigDecimal

object EffectiveLineTotal {
    fun of(saleLine: SaleSessionLine, lineAdjustments: List<SaleSessionAdjustment>): BigDecimal {
        val netAdjustmentAmount = lineAdjustments.sumOf { adjustment ->
            val amount = SaleSessionTotalsCalculator.calculateAdjustmentAmount(adjustment, listOf(saleLine))
            if (adjustment.direction == AdjustmentDirection.DISCOUNT) amount.negate() else amount
        }
        return saleLine.lineTotal.add(netAdjustmentAmount)
    }
}
