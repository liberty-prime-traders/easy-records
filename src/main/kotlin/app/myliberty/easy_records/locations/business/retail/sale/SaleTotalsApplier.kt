package app.myliberty.easy_records.locations.business.sale

import app.myliberty.easy_records.locations.business.sale_adjustment.api.SaleAdjustmentSummaryDto
import app.myliberty.easy_records.organizations.business.adjustment_reason.api.AdjustmentDirection
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class SaleTotalsApplier {

    fun applyTotals(
        sale: SaleEntity,
        saleLines: List<SaleLineEntity>,
        adjustmentSummaries: List<SaleAdjustmentSummaryDto>,
    ) {
        sale.subtotal = saleLines.sumOf { it.lineTotal }
        sale.lineLevelDiscountTotal = sumAdjustmentAmounts(adjustmentSummaries, AdjustmentDirection.DISCOUNT, lineLevel = true)
        sale.orderLevelDiscountTotal = sumAdjustmentAmounts(adjustmentSummaries, AdjustmentDirection.DISCOUNT, lineLevel = false)
        sale.lineLevelSurchargeTotal = sumAdjustmentAmounts(adjustmentSummaries, AdjustmentDirection.SURCHARGE, lineLevel = true)
        sale.orderLevelSurchargeTotal = sumAdjustmentAmounts(adjustmentSummaries, AdjustmentDirection.SURCHARGE, lineLevel = false)
    }

    private fun sumAdjustmentAmounts(
        adjustmentSummaries: List<SaleAdjustmentSummaryDto>,
        direction: AdjustmentDirection,
        lineLevel: Boolean,
    ): BigDecimal = adjustmentSummaries
        .filter { it.direction == direction && (it.saleLineId != null) == lineLevel }
        .sumOf { it.calculatedAmount }
}
