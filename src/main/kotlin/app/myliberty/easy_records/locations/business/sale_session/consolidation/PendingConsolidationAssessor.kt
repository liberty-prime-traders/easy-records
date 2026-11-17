package app.myliberty.easy_records.locations.business.sale_session.consolidation

import app.myliberty.easy_records.locations.business.sale_session.api.ConsolidationIssueResponse
import app.myliberty.easy_records.locations.business.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge.ConsolidationProblemStoryWriter
import app.myliberty.easy_records.locations.business.sale_session.consolidation.line_merge.SaleLineMerger
import app.myliberty.easy_records.organizations.business.unitconversion.api.UnitConversionGraph
import java.util.UUID

object PendingConsolidationAssessor {

    fun assess(
        saleSession: SaleSession,
        inactiveLocationProductIds: Set<UUID>,
        unitConversionGraph: UnitConversionGraph,
    ): PendingConsolidationAssessment {
        val pendingConsolidation = saleSession.requiredPendingConsolidation()
        val mergeResult = SaleLineMerger.merge(
            saleLines = saleSession.saleLines,
            saleAdjustments = saleSession.saleAdjustments,
            inactiveLocationProductIds = inactiveLocationProductIds,
            unitConversionGraph = unitConversionGraph,
        )
        val consolidationIssues = ConsolidationProblemStoryWriter.write(
            consolidationProblems = mergeResult.problems,
            problemSaleLines = mergeResult.problemSaleLines,
            problemSaleAdjustments = mergeResult.problemSaleAdjustments,
            sourceSaleReferenceByLineKey = pendingConsolidation.sourceSaleReferenceByLineKey,
            unitNameByUnitId = unitConversionGraph.getBreakdownUnits().associate { it.id to it.name },
        ).map { blockingProblem ->
            ConsolidationIssueResponse(
                saleReferences = blockingProblem.documentReference,
                description = blockingProblem.description,
                fix = blockingProblem.effect,
            )
        }
        val saleReferencesWithOrderLevelAdjustments = pendingConsolidation.sourceSales
            .map { it.referenceNumber }
            .filter { it in pendingConsolidation.saleReferencesWithOrderLevelAdjustments }
        val orderLevelWarnings = if (saleReferencesWithOrderLevelAdjustments.isEmpty()) {
            emptyList()
        } else {
            val warning = ConsolidationProblemStoryWriter
                .writeDroppedOrderLevelAdjustments(saleReferencesWithOrderLevelAdjustments)
            listOf(
                ConsolidationIssueResponse(
                    saleReferences = warning.documentReference,
                    description = warning.description,
                    fix = warning.effect,
                    blocking = false,
                )
            )
        }
        return PendingConsolidationAssessment(consolidationIssues + orderLevelWarnings)
    }
}
