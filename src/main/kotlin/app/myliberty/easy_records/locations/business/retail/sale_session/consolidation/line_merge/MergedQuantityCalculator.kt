package app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.quantity.QuantityLimits
import java.math.BigDecimal

object MergedQuantityCalculator {

    sealed interface QuantityOutcome {
        data class Combined(val quantity: BigDecimal) : QuantityOutcome
        data object NotExact : QuantityOutcome
        data object TooPrecise : QuantityOutcome
        data object TooLarge : QuantityOutcome
    }

    fun calculate(
        productLines: List<SaleSessionLine>,
        targetUnit: TargetUnit,
        targetIsTheSharedUnit: Boolean,
    ): QuantityOutcome {
        val mergedQuantity = if (targetIsTheSharedUnit) {
            productLines.sumOf { it.userProvidedQuantity }
        } else {
            val totalStorageQuantity = productLines.sumOf {
                it.userProvidedQuantity.multiply(BigDecimal.valueOf(it.storageUnitsPerUserProvidedUnit))
            }
            val targetUnitFactor = BigDecimal.valueOf(targetUnit.storageUnitsPerUnit)
            Decimals.exactQuotientOrNull(totalStorageQuantity, targetUnitFactor) ?: return QuantityOutcome.NotExact
        }
        val decimalPlaces = mergedQuantity.stripTrailingZeros().scale()
        if (decimalPlaces > QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES) return QuantityOutcome.TooPrecise
        if (mergedQuantity > QuantityLimits.MAX_USER_QUANTITY) return QuantityOutcome.TooLarge
        return QuantityOutcome.Combined(mergedQuantity)
    }
}
