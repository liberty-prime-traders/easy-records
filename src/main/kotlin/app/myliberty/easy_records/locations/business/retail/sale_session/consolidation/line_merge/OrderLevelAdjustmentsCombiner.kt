package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import java.math.BigDecimal
import java.util.UUID

object OrderLevelAdjustmentsCombiner {

    private const val PERCENTAGE_DECIMAL_PLACES = 4

    // A percentage of one source sale is money on that sale, so the sources' amounts are added as money and turned back
    // into one percentage of the combined goods total, and only when that percentage is exact. The approver is part of
    // the key so an approval is never attributed to an adjustment its approver did not see.
    fun combine(
        orderLevelSaleAdjustments: List<SaleSessionAdjustment>,
        sourceAmountByAdjustmentKey: Map<UUID, BigDecimal>,
        combinedSubtotal: BigDecimal,
    ): List<SaleSessionAdjustment> {
        if (combinedSubtotal.signum() == 0) return emptyList()
        return orderLevelSaleAdjustments
            .groupBy { OrderLevelAdjustmentKey.of(it) }
            .values
            .map { sameKindSaleAdjustments ->
                val sourceAmountTotal = sameKindSaleAdjustments.sumOf {
                    sourceAmountByAdjustmentKey.getValue(it.identity.key())
                }
                sameKindSaleAdjustments.first().copy(
                    identity = SessionIdentity.mintFreshIdentity(),
                    value = exactPercentage(sourceAmountTotal, combinedSubtotal),
                    note = sameKindSaleAdjustments
                        .mapNotNull { StringUtils.getValueOrNull(it.note) }
                        .distinct()
                        .joinToString(separator = "; ")
                        .ifEmpty { null },
                )
            }
    }

    fun canCombine(
        orderLevelSaleAdjustments: List<SaleSessionAdjustment>,
        sourceAmountByAdjustmentKey: Map<UUID, BigDecimal>,
        combinedSubtotal: BigDecimal,
    ): Boolean {
        if (combinedSubtotal.signum() == 0) return true
        return orderLevelSaleAdjustments
            .groupBy { OrderLevelAdjustmentKey.of(it) }
            .values
            .all { sameKindSaleAdjustments ->
                val sourceAmountTotal = sameKindSaleAdjustments.sumOf {
                    sourceAmountByAdjustmentKey.getValue(it.identity.key())
                }
                exactPercentageOrNull(sourceAmountTotal, combinedSubtotal) != null
            }
    }

    private fun exactPercentage(sourceAmountTotal: BigDecimal, combinedSubtotal: BigDecimal): BigDecimal =
        exactPercentageOrNull(sourceAmountTotal, combinedSubtotal) ?: throw LibertyGenericException(
            "The sale-wide discounts or surcharges of these sales cannot be added into one exactly. " +
                "Remove them from the sales, or consolidate fewer sales"
        )

    private fun exactPercentageOrNull(sourceAmountTotal: BigDecimal, combinedSubtotal: BigDecimal): BigDecimal? {
        val percentage = Decimals.exactQuotientOrNull(sourceAmountTotal.multiply(BigDecimal(100)), combinedSubtotal)
            ?.stripTrailingZeros() ?: return null
        val percentageFits = percentage.scale() <= PERCENTAGE_DECIMAL_PLACES
        val amountOfPercentage = Decimals.MoneyScale.divide(percentage.multiply(combinedSubtotal), BigDecimal(100))
        val amountIsPreserved = amountOfPercentage.compareTo(sourceAmountTotal) == 0
        if (!percentageFits || !amountIsPreserved) return null
        return if (percentage.scale() < 0) percentage.setScale(0) else percentage
    }
}

private data class OrderLevelAdjustmentKey(
    val adjustmentReasonId: UUID,
    val direction: AdjustmentDirection,
    val calculationMethod: PriceAdjustmentMethod,
    val approvedById: UUID?,
) {
    companion object {
        fun of(saleAdjustment: SaleSessionAdjustment) = OrderLevelAdjustmentKey(
            adjustmentReasonId = saleAdjustment.adjustmentReasonId,
            direction = saleAdjustment.direction,
            calculationMethod = saleAdjustment.calculationMethod,
            approvedById = saleAdjustment.approvedById,
        )
    }
}
