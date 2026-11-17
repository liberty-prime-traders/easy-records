package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionBuilder
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionCopier
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionStockOverlay
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionUpdateFinalizer
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge.OrderLevelAdjustmentsCombiner
import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.sale_session.api.ConsolidationSourceSale
import app.myliberty.easy_records.locations.business.retail.sale_session.api.PendingConsolidation
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionResponseDto
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
