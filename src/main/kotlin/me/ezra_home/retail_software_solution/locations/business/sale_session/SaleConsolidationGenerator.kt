package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.sale.api.DraftSalePersister
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleHeaderDto
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleReferenceAllocator
import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleUpdater
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionResponseDto
import me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge.OrderLevelAdjustmentsCombiner
import me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge.SaleLineMergeResult
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Component
class SaleConsolidationGenerator(
    private val consolidationSources: ConsolidationSources,
    private val consolidationSourcePayments: ConsolidationSourcePayments,
    private val consolidationSessionCleanup: ConsolidationSessionCleanup,
    private val saleUpdater: SaleUpdater,
    private val draftSalePersister: DraftSalePersister,
    private val saleReferenceAllocator: SaleReferenceAllocator,
    private val saleSessionCopier: SaleSessionCopier,
    private val saleConsolidationMerger: SaleConsolidationMerger,
    private val saleSessionValidator: SaleSessionValidator,
    private val saleSessionLoader: SaleSessionLoader,
    private val saleSessionUpdateFinalizer: SaleSessionUpdateFinalizer,
    private val saleSessionAssembler: SaleSessionAssembler,
    private val sessionToSaveRequestMapper: SessionToSaveRequestMapper,
) {

    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun generate(consolidationGenerateInput: ConsolidationGenerateInput): SaleSessionResponseDto {
        val sourceSaleHeaders = consolidationSources.lock(consolidationGenerateInput.sourceSales.map { it.saleId })
        consolidationSources.guardUnchangedSinceSelection(sourceSaleHeaders, consolidationGenerateInput.sourceSales)
        consolidationSources.guardSelectable(sourceSaleHeaders)
        val consolidationCopy = saleSessionCopier
            .copyFromSalesWithSourceReferences(UUID.randomUUID(), sourceSaleHeaders)
        val mergeResult = saleConsolidationMerger
            .merge(consolidationCopy, consolidationGenerateInput.reviewedProductLines)
        return generateFromMerged(
            sourceSaleHeaders, consolidationCopy, mergeResult, consolidationGenerateInput.pendingSessionId
        )
    }

    // The caller has locked the sources and merged their lines in this transaction, so nothing is read twice.
    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun generateFromMerged(
        sourceSaleHeaders: List<SaleHeaderDto>,
        consolidationCopy: ConsolidationCopy,
        mergeResult: SaleLineMergeResult,
        pendingSessionId: UUID?,
    ): SaleSessionResponseDto {
        if (mergeResult.problems.isNotEmpty()) {
            throw RtsGenericException("Some products still cannot be combined. Fix the issues listed first")
        }
        val activeSourcePayments = consolidationSourcePayments.activePaymentsOf(sourceSaleHeaders)
        val consolidatedSession = consolidationCopy.saleSession.copy(
            saleLines = mergeResult.mergedSaleLines,
            saleAdjustments = mergeResult.mergedSaleAdjustments +
                OrderLevelAdjustmentsCombiner.combine(
                    orderLevelSaleAdjustments = mergeResult.orderLevelSaleAdjustments,
                    sourceAmountByAdjustmentKey = consolidationCopy.orderLevelAmountByAdjustmentKey,
                    combinedSubtotal = mergeResult.mergedSaleLines.sumOf { it.lineTotal },
                ),
            salePayments = consolidationSourcePayments.asNewSessionPayments(activeSourcePayments),
        )
        val preparedSession = saleSessionUpdateFinalizer.prepare(consolidatedSession)
        saleSessionValidator.guardNonEmptyLines(preparedSession)
        saleSessionValidator.guardNoInactiveProducts(preparedSession)
        saleSessionValidator.guardPaymentsWithinTotal(preparedSession)

        val consolidatedReferenceNumber = saleReferenceAllocator.allocate()
        consolidationSourcePayments.voidAll(activeSourcePayments, consolidatedReferenceNumber)
        saleUpdater.voidSourcesOfConsolidation(sourceSaleHeaders.map { it.id }, consolidatedReferenceNumber)
        val saleSaveRequest = sessionToSaveRequestMapper.toSaleSaveRequest(preparedSession)
            .copy(preAllocatedReferenceNumber = consolidatedReferenceNumber)
        val saleSaveResult = draftSalePersister.saveDraft(saleSaveRequest)

        val newSession = saleSessionUpdateFinalizer.prepare(
            saleSessionLoader.loadFromSale(UUID.randomUUID(), saleSaveResult.saleId)
        )
        consolidationSessionCleanup.replaceAfterCommit(newSession, sourceSaleHeaders, pendingSessionId)
        return saleSessionAssembler.buildResponse(newSession)
    }
}
