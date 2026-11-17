package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.retail.sale_session.api.LineQuantities
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.organizations.business.retail.unitconversion.api.UnitConversionGraph
import java.math.BigDecimal

/** Merges the lines of one product; accepted only when the effective total equals the sources' sum exactly. */
object PerProductLinesMerger {

    fun merge(
        linesWithAdjustments: List<LineWithAdjustments>,
        productIsInactive: Boolean,
        unitConversionGraph: UnitConversionGraph,
    ): ProductMergeOutcome {
        if (productIsInactive) return ProductMergeOutcome.Blocked(ConsolidationProblemKind.INACTIVE_PRODUCT)
        val onlyLine = linesWithAdjustments.singleOrNull()
        if (onlyLine != null) return ProductMergeOutcome.Merged(onlyLine.saleLine, onlyLine.allAdjustments)

        val productLines = linesWithAdjustments.map { it.saleLine }
        val firstLine = productLines.first()
        val listPricesDiffer = productLines.any { it.defaultSalePrice.compareTo(firstLine.defaultSalePrice) != 0 }
        // A typed price replaces the list price in the effective total, so list prices only matter without it.
        val everyLineHasPriceOverride = linesWithAdjustments.all { it.priceOverride != null }
        val listPricesBlockTheMerge = listPricesDiffer && !everyLineHasPriceOverride
        if (listPricesBlockTheMerge) {
            return ProductMergeOutcome.Blocked(ConsolidationProblemKind.DIFFERENT_LIST_PRICES)
        }

        val storageUnitsPerBaseUnit = unitConversionGraph.getSizeInRoot(firstLine.baseUnitId)
        val outcomes = candidateUnits(productLines, unitConversionGraph).map { targetUnit ->
            mergeInUnit(linesWithAdjustments, targetUnit, storageUnitsPerBaseUnit)
        }
        val mergedOutcome = outcomes.firstOrNull { it is ProductMergeOutcome.Merged }
        return mergedOutcome ?: outcomeThatFailedLatest(outcomes)
    }

    private fun outcomeThatFailedLatest(outcomes: List<ProductMergeOutcome>): ProductMergeOutcome =
        outcomes.filterIsInstance<ProductMergeOutcome.Blocked>().maxBy { it.kind.failedAtStep.order }

    // A readable unit wins over the storage unit: selected units first, then the tree from the largest down.
    private fun candidateUnits(
        productLines: List<SaleSessionLine>,
        unitConversionGraph: UnitConversionGraph,
    ): List<TargetUnit> {
        val selectedUnits = productLines
            .distinctBy { it.storageUnitsPerUserProvidedUnit }
            .sortedBy { it.storageUnitsPerUserProvidedUnit }
            .map { TargetUnit(it.unitId, it.storageUnitsPerUserProvidedUnit) }
        val selectedSizes = selectedUnits.map { it.storageUnitsPerUnit }.toSet()
        val storageUnitId = unitConversionGraph.getStorageUnitId(productLines.first().baseUnitId)
        val otherUnits = unitConversionGraph.getBreakdownUnits()
            .filter { it.rootUnitId == storageUnitId && it.sizeInRoot !in selectedSizes }
            .distinctBy { it.sizeInRoot }
            .sortedByDescending { it.sizeInRoot }
            .map { TargetUnit(it.id, it.sizeInRoot) }
        return selectedUnits + otherUnits
    }

    private fun mergeInUnit(
        linesWithAdjustments: List<LineWithAdjustments>,
        targetUnit: TargetUnit,
        storageUnitsPerBaseUnit: Long,
    ): ProductMergeOutcome {
        val productLines = linesWithAdjustments.map { it.saleLine }
        val firstLine = productLines.first()
        // A unit's size can be edited after a sale snapshots it, so the snapshot decides, not the unit id.
        val targetIsTheSharedUnit = productLines.all {
            it.unitId == targetUnit.unitId && it.storageUnitsPerUserProvidedUnit == targetUnit.storageUnitsPerUnit
        }
        val quantityOutcome = MergedQuantityCalculator.calculate(productLines, targetUnit, targetIsTheSharedUnit)
        val mergedQuantity = when (quantityOutcome) {
            is MergedQuantityCalculator.QuantityOutcome.Combined -> quantityOutcome.quantity
            is MergedQuantityCalculator.QuantityOutcome.NotExact ->
                return ProductMergeOutcome.Blocked(ConsolidationProblemKind.UNITS_NOT_EXACT)
            is MergedQuantityCalculator.QuantityOutcome.TooPrecise ->
                return ProductMergeOutcome.Blocked(ConsolidationProblemKind.QUANTITY_TOO_PRECISE)
            is MergedQuantityCalculator.QuantityOutcome.TooLarge ->
                return ProductMergeOutcome.Blocked(ConsolidationProblemKind.QUANTITY_TOO_LARGE)
        }

        val mergedLineTotal = if (targetIsTheSharedUnit) {
            productLines.sumOf { it.lineTotal }
        } else {
            val lineQuantities = LineQuantities(
                userProvidedQuantity = mergedQuantity,
                storageUnitsPerUserProvidedUnit = targetUnit.storageUnitsPerUnit,
                storageUnitsPerBaseUnit = storageUnitsPerBaseUnit,
            )
            SaleSessionLine.lineTotal(firstLine.defaultSalePrice, lineQuantities)
        }
        val mergedLine = firstLine.copy(
            identity = SessionIdentity.mintFreshIdentity(),
            userProvidedQuantity = mergedQuantity,
            unitId = targetUnit.unitId,
            storageUnitsPerUserProvidedUnit = targetUnit.storageUnitsPerUnit,
            lineTotal = mergedLineTotal,
            storageQuantityOnHand = BigDecimal.ZERO,
            storageQuantityReserved = BigDecimal.ZERO,
            storageQuantityAvailable = BigDecimal.ZERO,
        )

        val adjustmentsOutcome = MergedAdjustmentsBuilder.build(linesWithAdjustments, mergedLine, targetUnit)
        val mergedAdjustments = when (adjustmentsOutcome) {
            is MergedAdjustmentsBuilder.AdjustmentsOutcome.Combined -> adjustmentsOutcome.saleAdjustments
            is MergedAdjustmentsBuilder.AdjustmentsOutcome.Blocked ->
                return ProductMergeOutcome.Blocked(adjustmentsOutcome.kind)
        }

        val sourceEffectiveTotal = linesWithAdjustments.sumOf { EffectiveLineTotal.of(it.saleLine, it.allAdjustments) }
        val mergedEffectiveTotal = EffectiveLineTotal.of(mergedLine, mergedAdjustments)
        if (sourceEffectiveTotal.compareTo(mergedEffectiveTotal) != 0) {
            return ProductMergeOutcome.Blocked(ConsolidationProblemKind.TOTAL_DRIFT)
        }
        return ProductMergeOutcome.Merged(mergedLine, mergedAdjustments)
    }
}
