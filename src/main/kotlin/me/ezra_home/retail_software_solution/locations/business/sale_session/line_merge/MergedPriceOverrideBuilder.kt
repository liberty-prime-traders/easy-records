package me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAdjustment
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SessionIdentity
import me.ezra_home.retail_software_solution.locations.business.location_product.api.LinePricing
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.business.Decimals
import java.math.BigDecimal

object MergedPriceOverrideBuilder {

    sealed interface PriceOverrideOutcome {
        data object NoOverride : PriceOverrideOutcome
        data object OnSomeLinesOnly : PriceOverrideOutcome
        data object PricesDiffer : PriceOverrideOutcome
        data object PriceNotTypeable : PriceOverrideOutcome
        data object ApproversDiffer : PriceOverrideOutcome
        data class Overridden(val saleAdjustment: SaleSessionAdjustment) : PriceOverrideOutcome
    }

    fun build(
        linesWithAdjustments: List<LineWithAdjustments>,
        mergedLine: SaleSessionLine,
        targetUnit: TargetUnit,
    ): PriceOverrideOutcome {
        val priceOverrides = linesWithAdjustments.mapNotNull { it.priceOverride }
        if (priceOverrides.isEmpty()) return PriceOverrideOutcome.NoOverride
        val everyLineHasAnOverride = priceOverrides.size == linesWithAdjustments.size
        if (!everyLineHasAnOverride) return PriceOverrideOutcome.OnSomeLinesOnly

        val typedPricesPerTargetUnit = linesWithAdjustments.map { lineWithAdjustments ->
            val typedPrice = checkNotNull(lineWithAdjustments.priceOverride).value
            val lineUnitFactor = lineWithAdjustments.saleLine.storageUnitsPerUserProvidedUnit
            val targetUnitFactor = targetUnit.storageUnitsPerUnit
            // The typed price of a line is per its own unit; the merged line is priced per the target unit.
            Decimals.exactQuotientOrNull(
                typedPrice.multiply(BigDecimal.valueOf(targetUnitFactor)),
                BigDecimal.valueOf(lineUnitFactor),
            ) ?: return PriceOverrideOutcome.PriceNotTypeable
        }
        val mergedTypedPrice = typedPricesPerTargetUnit.first()
        val pricesAgree = typedPricesPerTargetUnit.all { it.compareTo(mergedTypedPrice) == 0 }
        val priceFitsMoneyScale = mergedTypedPrice.stripTrailingZeros().scale() <= Decimals.MONEY_SCALE
        val approversAgree = priceOverrides.all { it.approvedById == priceOverrides.first().approvedById }
        if (!pricesAgree) return PriceOverrideOutcome.PricesDiffer
        if (!priceFitsMoneyScale) return PriceOverrideOutcome.PriceNotTypeable
        if (!approversAgree) return PriceOverrideOutcome.ApproversDiffer

        val difference = LinePricing.overrideDifference(
            mergedLine.lineTotal, mergedTypedPrice, mergedLine.userProvidedQuantity
        )
        if (difference.signum() == 0) return PriceOverrideOutcome.NoOverride
        val firstOverride = priceOverrides.first()
        val direction = if (difference.signum() > 0) AdjustmentDirection.DISCOUNT else AdjustmentDirection.SURCHARGE
        return PriceOverrideOutcome.Overridden(
            firstOverride.copy(
                identity = SessionIdentity.mintFreshIdentity(),
                relatedSaleLineIdentity = mergedLine.identity,
                direction = direction,
                value = mergedTypedPrice,
            )
        )
    }
}
