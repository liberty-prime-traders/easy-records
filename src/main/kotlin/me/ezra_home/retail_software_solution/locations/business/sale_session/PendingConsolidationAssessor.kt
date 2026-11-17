package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.ConsolidationIssueResponse
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge.ConsolidationProblemStoryWriter
import me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge.SaleLineMerger
import me.ezra_home.retail_software_solution.organizations.business.unitconversion.api.UnitConversionGraph
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
        val heldProductIssues = ConsolidationProblemStoryWriter.write(
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
        val inactiveCleanProductIssues = pendingConsolidation.cleanProducts
            .filter { it.locationProductId in inactiveLocationProductIds }
            .map { cleanProduct ->
                ConsolidationProblemStoryWriter
                    .writeInactiveCleanProduct(cleanProduct.productLabel, cleanProduct.sourceSaleReferences)
                    .let { blockingProblem ->
                        ConsolidationIssueResponse(
                            saleReferences = blockingProblem.documentReference,
                            description = blockingProblem.description,
                            fix = blockingProblem.effect,
                        )
                    }
            }
        val consolidationIssues = (heldProductIssues + inactiveCleanProductIssues)
            .sortedWith(compareBy({ it.saleReferences }, { it.description }))
        return PendingConsolidationAssessment(consolidationIssues)
    }
}
