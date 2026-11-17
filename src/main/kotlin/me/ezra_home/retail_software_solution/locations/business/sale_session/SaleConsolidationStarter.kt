package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge.SaleLineMergeResult
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.PendingCleanProduct
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
        if (mergeResult.problems.isEmpty()) {
            return saleConsolidationGenerator.generateFromMerged(
                sourceSaleHeaders, consolidationCopy, mergeResult, pendingSessionId = null
            )
        }
        val copiedSession = consolidationCopy.saleSession
        val pendingConsolidation = PendingConsolidation(
            sourceSales = sourceSales,
            sourceSaleReferenceByLineKey = consolidationCopy.sourceSaleReferenceByLineKey,
            stockHeldBySourceSalesByLocationProductId = PendingConsolidationSourceStock.calculate(
                sourceSaleHeaders, consolidationCopy.sourceSaleLinesBySaleId
            ),
            cleanProducts = cleanProductsOf(consolidationCopy, mergeResult),
        )
        val pendingSession = SaleSessionBuilder(copiedSession.sessionId)
            .asPendingConsolidation(pendingConsolidation)
            .withHeader(copiedSession.header)
            .withSaleLines(mergeResult.problemSaleLines)
            .withSaleAdjustments(mergeResult.problemSaleAdjustments)
            .build()
        return saleSessionUpdateFinalizer.finalize(saleSessionStockOverlay.populate(pendingSession))
    }

    private fun cleanProductsOf(
        consolidationCopy: ConsolidationCopy,
        mergeResult: SaleLineMergeResult,
    ): List<PendingCleanProduct> {
        val problemLocationProductIds = mergeResult.problemSaleLines.map { it.locationProductId }.toSet()
        return consolidationCopy.saleSession.saleLines
            .filter { it.locationProductId !in problemLocationProductIds }
            .groupBy { it.locationProductId }
            .map { (locationProductId, cleanSaleLines) ->
                PendingCleanProduct(
                    locationProductId = locationProductId,
                    productLabel = cleanSaleLines.first().productLabel,
                    sourceSaleReferences = cleanSaleLines
                        .map { consolidationCopy.sourceSaleReferenceByLineKey.getValue(it.lineKey) }
                        .distinct(),
                )
            }
    }
}
