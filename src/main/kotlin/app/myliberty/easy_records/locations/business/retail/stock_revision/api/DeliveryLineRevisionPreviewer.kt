package app.myliberty.easy_records.locations.business.retail.stock_revision.api

import app.myliberty.easy_records.configuration.datasource.TransactionalOnLocationSchema
import app.myliberty.easy_records.locations.business.retail.stock_revision.AdjustmentCostChangeResolver
import app.myliberty.easy_records.locations.business.retail.stock_revision.ConsumerCostChange
import app.myliberty.easy_records.locations.business.retail.stock_revision.DeliveryLineRevisionAssessor
import app.myliberty.easy_records.locations.business.retail.stock_revision.RevisionGuidanceWriter
import app.myliberty.easy_records.locations.business.retail.stock_revision.RevisionLedgerPreviewWriter
import app.myliberty.easy_records.locations.business.retail.stock_revision.RevisionPreviewStoryWriter
import org.springframework.stereotype.Service

// Takes no lock, so a preview can differ from the save, which assesses again under the product lock.
@Service
@TransactionalOnLocationSchema(readOnly = true)
class DeliveryLineRevisionPreviewer(
    private val deliveryLineRevisionAssessor: DeliveryLineRevisionAssessor,
    private val revisionLedgerPreviewWriter: RevisionLedgerPreviewWriter,
    private val adjustmentCostChangeResolver: AdjustmentCostChangeResolver,
) {

    fun preview(deliveryLineRevisionRequest: DeliveryLineRevisionRequest): DeliveryLineRevisionPreviewDto {
        val assessment = deliveryLineRevisionAssessor.assess(deliveryLineRevisionRequest)
        val validatedDeliveryLineRevision = assessment.validatedDeliveryLineRevision
        val consumerReplayPlans = assessment.replayResult.consumerReplayPlans
        val adjustmentCostChangeByReference = if (validatedDeliveryLineRevision.problems.isEmpty()) {
            adjustmentCostChangeResolver.byAdjustmentReference(ConsumerCostChange.ofPlans(consumerReplayPlans))
        } else {
            emptyMap()
        }
        val transactions = if (validatedDeliveryLineRevision.problems.isNotEmpty()) {
            validatedDeliveryLineRevision.problems.map(RevisionPreviewStoryWriter::blockingItem)
        } else {
            RevisionPreviewStoryWriter.write(
                assessment.replayResult, assessment.reservationProblems, assessment.quantityBreakdown,
                assessment.replacementStockEntryId, adjustmentCostChangeByReference
            )
        }
        val ledgerEntries = if (assessment.blockingProblems.isEmpty()) {
            revisionLedgerPreviewWriter.write(
                validatedDeliveryLineRevision, consumerReplayPlans, adjustmentCostChangeByReference.values
            )
        } else {
            emptyList()
        }
        return DeliveryLineRevisionPreviewDto(
            revisedDeliveryLineReferenceNumber = validatedDeliveryLineRevision.revisedLineReferenceNumber,
            changeSummary = RevisionPreviewStoryWriter.changeSummary(
                validatedDeliveryLineRevision, assessment.quantityBreakdown
            ),
            transactions = transactions,
            guidance = RevisionGuidanceWriter.write(assessment),
            ledgerEntries = ledgerEntries,
        )
    }
}
