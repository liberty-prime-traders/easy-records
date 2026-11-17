package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.AdjustmentAmountCalculator
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.SaleAdjustmentCreateDto
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionTotals
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import java.math.BigDecimal
import java.util.UUID

object SaleSessionTotalsCalculator {

    fun recompute(saleSession: SaleSession): SaleSession {
        val totals = compute(saleSession.saleLines, saleSession.saleAdjustments, saleSession.totalPaid())
        return saleSession.copy(totals = totals)
    }

    fun compute(
        saleSessionLines: List<SaleSessionLine>,
        saleSessionAdjustments: List<SaleSessionAdjustment>,
        paymentTotal: BigDecimal,
    ): SaleSessionTotals {
        val subtotal = saleSessionLines.sumOf { it.lineTotal }
        val calculatedAmountByAdjustmentKey = saleSessionAdjustments.associate { saleSessionAdjustment ->
            saleSessionAdjustment.identity.key() to calculateAdjustmentAmount(saleSessionAdjustment, saleSessionLines)
        }
        val lineLevelDiscount = sumAdjustmentAmounts(
            saleSessionAdjustments, AdjustmentDirection.DISCOUNT, lineLevel = true, calculatedAmountByAdjustmentKey
        )
        val orderLevelDiscount = sumAdjustmentAmounts(
            saleSessionAdjustments, AdjustmentDirection.DISCOUNT, lineLevel = false, calculatedAmountByAdjustmentKey
        )
        val lineLevelSurcharge = sumAdjustmentAmounts(
            saleSessionAdjustments, AdjustmentDirection.SURCHARGE, lineLevel = true, calculatedAmountByAdjustmentKey
        )
        val orderLevelSurcharge = sumAdjustmentAmounts(
            saleSessionAdjustments, AdjustmentDirection.SURCHARGE, lineLevel = false, calculatedAmountByAdjustmentKey
        )
        val receivableTotal = subtotal - lineLevelDiscount - orderLevelDiscount + lineLevelSurcharge + orderLevelSurcharge
        return SaleSessionTotals(
            subtotal = subtotal,
            lineLevelDiscountTotal = lineLevelDiscount,
            orderLevelDiscountTotal = orderLevelDiscount,
            lineLevelSurchargeTotal = lineLevelSurcharge,
            orderLevelSurchargeTotal = orderLevelSurcharge,
            paymentTotal = paymentTotal,
            receivableTotal = receivableTotal,
            balance = receivableTotal - paymentTotal,
        )
    }

    fun calculateAdjustmentAmount(
        saleSessionAdjustment: SaleSessionAdjustment,
        saleSessionLines: List<SaleSessionLine>,
    ): BigDecimal {
        val adjustmentCreateDto = SaleAdjustmentCreateDto(
            saleLineKey = saleSessionAdjustment.relatedSaleLineIdentity?.key(),
            direction = saleSessionAdjustment.direction,
            calculationMethod = saleSessionAdjustment.calculationMethod,
            value = saleSessionAdjustment.value,
            adjustmentReasonId = saleSessionAdjustment.adjustmentReasonId,
            note = saleSessionAdjustment.note,
            approvedById = saleSessionAdjustment.approvedById,
        )
        return AdjustmentAmountCalculator.calculateAmount(adjustmentCreateDto, saleSessionLines)
    }

    private fun sumAdjustmentAmounts(
        saleSessionAdjustments: List<SaleSessionAdjustment>,
        direction: AdjustmentDirection,
        lineLevel: Boolean,
        calculatedAmountByAdjustmentKey: Map<UUID, BigDecimal>,
    ): BigDecimal = saleSessionAdjustments
        .filter { it.direction == direction && (it.relatedSaleLineIdentity != null) == lineLevel }
        .sumOf { calculatedAmountByAdjustmentKey.getValue(it.identity.key()) }
}
