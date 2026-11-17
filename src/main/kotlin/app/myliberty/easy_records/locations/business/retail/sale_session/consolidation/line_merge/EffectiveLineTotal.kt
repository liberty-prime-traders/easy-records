package app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.sale_session.SaleSessionTotalsCalculator
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentDirection
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
