package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.sale.api.DraftSalePersister
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleReferenceAllocator
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleUpdater
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionAssembler
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionCopier
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionLoader
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionUpdateFinalizer
import app.myliberty.easy_records.locations.business.retail.sale_session.SaleSessionValidator
import app.myliberty.easy_records.locations.business.retail.sale_session.SessionToSaveRequestMapper
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionResponseDto
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge.OrderLevelAdjustmentsCombiner
import app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge.SaleLineMergeResult
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
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
            sourceSaleHeaders,
            consolidationCopy,
            mergeResult,
            consolidationGenerateInput.pendingSessionId,
            keepOrderLevelAdjustments = false,
        )
    }

    // The caller has locked the sources and merged their lines in this transaction, so nothing is read twice.
    @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
    fun generateFromMerged(
        sourceSaleHeaders: List<SaleHeaderDto>,
        consolidationCopy: ConsolidationCopy,
        mergeResult: SaleLineMergeResult,
        pendingSessionId: UUID?,
        keepOrderLevelAdjustments: Boolean,
    ): SaleSessionResponseDto {
        if (mergeResult.problems.isNotEmpty()) {
            throw LibertyGenericException("Some products still cannot be combined. Fix the issues listed first")
        }
        val activeSourcePayments = consolidationSourcePayments.activePaymentsOf(sourceSaleHeaders)
        val carriedOrderLevelAdjustments = if (keepOrderLevelAdjustments) {
            OrderLevelAdjustmentsCombiner.combine(
                orderLevelSaleAdjustments = mergeResult.orderLevelSaleAdjustments,
                sourceAmountByAdjustmentKey = consolidationCopy.orderLevelAmountByAdjustmentKey,
                combinedSubtotal = mergeResult.mergedSaleLines.sumOf { it.lineTotal },
            )
        } else {
            emptyList()
        }
        val consolidatedSession = consolidationCopy.saleSession.copy(
            saleLines = mergeResult.mergedSaleLines,
            saleAdjustments = mergeResult.mergedSaleAdjustments + carriedOrderLevelAdjustments,
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
