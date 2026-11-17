package me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation

import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionBuilder
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionCopier
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionStockOverlay
import me.ezra_home.retail_software_solution.locations.business.sale_session.SaleSessionUpdateFinalizer
import me.ezra_home.retail_software_solution.locations.business.sale_session.consolidation.line_merge.OrderLevelAdjustmentsCombiner
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.ConsolidationSourceSale
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.PendingConsolidation
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class SaleConsolidationStarter(
    private val consolidationSources: ConsolidationSources,
    private val saleSessionCopier: SaleSessionCopier,
    private val saleConsolidationMerger: SaleConsolidationMerger,
    private val saleConsolidationGenerator: SaleConsolidationGenerator,
    private val saleSessionStockOverlay: SaleSessionStockOverlay,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
) {

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun start(saleIds: List<UUID>): SaleSessionResponseDto {
        val sourceSaleHeaders = consolidationSources.lock(saleIds)
        consolidationSources.guardSelectable(sourceSaleHeaders)
        val sourceSales = sourceSaleHeaders.map {
            ConsolidationSourceSale(saleId = it.id, saleVersion = it.version, referenceNumber = it.referenceNumber)
        }

        val consolidationCopy = saleSessionCopier
            .copyFromSalesWithSourceReferences(UUID.randomUUID(), sourceSaleHeaders)
        val mergeResult = saleConsolidationMerger.merge(consolidationCopy, ReviewedProductLines.NONE)
        val mergedSubtotal = mergeResult.mergedSaleLines.sumOf { it.lineTotal }
        val orderLevelAdjustmentsCombine = OrderLevelAdjustmentsCombiner.canCombine(
            mergeResult.orderLevelSaleAdjustments, consolidationCopy.orderLevelAmountByAdjustmentKey, mergedSubtotal
        )
        if (mergeResult.problems.isEmpty() && orderLevelAdjustmentsCombine) {
            return saleConsolidationGenerator.generateFromMerged(
                sourceSaleHeaders, consolidationCopy, mergeResult, pendingSessionId = null,
                keepOrderLevelAdjustments = true,
            )
        }
        val copiedSession = consolidationCopy.saleSession
        val pendingConsolidation = PendingConsolidation(
            sourceSales = sourceSales,
            sourceSaleReferenceByLineKey = consolidationCopy.sourceSaleReferenceByLineKey,
            stockHeldBySourceSalesByLocationProductId = PendingConsolidationSourceStock.calculate(
                sourceSaleHeaders, consolidationCopy.sourceSaleLinesBySaleId
            ),
            cleanLocationProductIds = mergeResult.mergedSaleLines.map { it.locationProductId }.toSet(),
            saleReferencesWithOrderLevelAdjustments = consolidationCopy.saleReferencesWithOrderLevelAdjustments,
        )
        val pendingSession = SaleSessionBuilder(copiedSession.sessionId)
            .asPendingConsolidation(pendingConsolidation)
            .withHeader(copiedSession.header)
            .withSaleLines(mergeResult.problemSaleLines)
            .withSaleAdjustments(mergeResult.problemSaleAdjustments)
            .build()
        return saleSessionUpdateFinalizer.finalize(saleSessionStockOverlay.populate(pendingSession))
    }
}
