package app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraph
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import java.util.UUID

object SaleLineMerger {

    fun merge(
        saleLines: List<SaleSessionLine>,
        saleAdjustments: List<SaleSessionAdjustment>,
        inactiveLocationProductIds: Set<UUID>,
        unitConversionGraph: UnitConversionGraph,
    ): SaleLineMergeResult {
        val saleLinesByLocationProductId = saleLines.groupBy { it.locationProductId }
        val (lineLevelSaleAdjustments, orderLevelSaleAdjustments) = saleAdjustments.partition {
            it.relatedSaleLineIdentity != null
        }
        val adjustmentsByLineKey = lineLevelSaleAdjustments.groupBy { it.requiredRelatedSaleLineKey() }
        val saleLineKeys = saleLines.mapTo(HashSet()) { it.identity.key() }
        if (adjustmentsByLineKey.keys.any { it !in saleLineKeys }) {
            throw RtsGenericException("Adjustment references a line that is not on the sale")
        }
        val firstSeenLocationProductIds = saleLines.map { it.locationProductId }.distinct()

        val mergedSaleLines = mutableListOf<SaleSessionLine>()
        val mergedSaleAdjustments = mutableListOf<SaleSessionAdjustment>()
        val problemSaleLines = mutableListOf<SaleSessionLine>()
        val problemSaleAdjustments = mutableListOf<SaleSessionAdjustment>()
        val problems = mutableListOf<ConsolidationProblem>()

        firstSeenLocationProductIds.forEach { locationProductId ->
            val productLines = saleLinesByLocationProductId.getValue(locationProductId)
            val linesWithAdjustments = productLines.map { line ->
                LineWithAdjustments.of(line, adjustmentsByLineKey[line.identity.key()].orEmpty())
            }
            val outcome = PerProductLinesMerger.merge(
                linesWithAdjustments = linesWithAdjustments,
                productIsInactive = locationProductId in inactiveLocationProductIds,
                unitConversionGraph = unitConversionGraph,
            )
            when (outcome) {
                is ProductMergeOutcome.Merged -> {
                    mergedSaleLines.add(outcome.saleLine)
                    mergedSaleAdjustments.addAll(outcome.saleAdjustments)
                }
                is ProductMergeOutcome.Blocked -> {
                    problemSaleLines.addAll(productLines)
                    problemSaleAdjustments.addAll(linesWithAdjustments.flatMap { it.allAdjustments })
                    problems.add(
                        ConsolidationProblem(locationProductId, outcome.kind, productLines.map { it.identity })
                    )
                }
            }
        }
        return SaleLineMergeResult(
            mergedSaleLines = mergedSaleLines,
            mergedSaleAdjustments = mergedSaleAdjustments,
            problemSaleLines = problemSaleLines,
            problemSaleAdjustments = problemSaleAdjustments,
            orderLevelSaleAdjustments = orderLevelSaleAdjustments,
            problems = problems,
        )
    }
}
