package app.myliberty.easy_records.locations.business.retail.sale_session.consolidation.line_merge

import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SessionIdentity
import app.myliberty.easy_records.locations.business.retail.sale_adjustment.api.PriceAdjustmentMethod
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import java.util.UUID

object MergedAdjustmentsBuilder {

    sealed interface AdjustmentsOutcome {
        data class Combined(val saleAdjustments: List<SaleSessionAdjustment>) : AdjustmentsOutcome
        data class Blocked(val kind: ConsolidationProblemKind) : AdjustmentsOutcome
    }

    fun build(
        linesWithAdjustments: List<LineWithAdjustments>,
        mergedLine: SaleSessionLine,
        targetUnit: TargetUnit,
    ): AdjustmentsOutcome {
        val mergedPriceOverride = MergedPriceOverrideBuilder.build(linesWithAdjustments, mergedLine, targetUnit)
        when (mergedPriceOverride) {
            is MergedPriceOverrideBuilder.PriceOverrideOutcome.NoOverride,
            is MergedPriceOverrideBuilder.PriceOverrideOutcome.Overridden -> Unit
            is MergedPriceOverrideBuilder.PriceOverrideOutcome.OnSomeLinesOnly ->
                return AdjustmentsOutcome.Blocked(ConsolidationProblemKind.OVERRIDE_ON_SOME_LINES_ONLY)
            is MergedPriceOverrideBuilder.PriceOverrideOutcome.PricesDiffer ->
                return AdjustmentsOutcome.Blocked(ConsolidationProblemKind.TYPED_PRICES_DIFFER)
            is MergedPriceOverrideBuilder.PriceOverrideOutcome.PriceNotTypeable ->
                return AdjustmentsOutcome.Blocked(ConsolidationProblemKind.TYPED_PRICE_NOT_TYPEABLE)
            is MergedPriceOverrideBuilder.PriceOverrideOutcome.ApproversDiffer ->
                return AdjustmentsOutcome.Blocked(ConsolidationProblemKind.APPROVERS_DIFFER)
        }

        val firstLineOtherAdjustments = linesWithAdjustments.first().otherAdjustments
        val firstLineSignatureCounts = signatureCountsOf(firstLineOtherAdjustments)
        val firstLineApprovedSignatureCounts = approvedSignatureCountsOf(firstLineOtherAdjustments)
        val everyLineCarriesTheSameAdjustments = linesWithAdjustments.all { lineWithAdjustments ->
            signatureCountsOf(lineWithAdjustments.otherAdjustments) == firstLineSignatureCounts
        }
        if (!everyLineCarriesTheSameAdjustments) {
            return AdjustmentsOutcome.Blocked(ConsolidationProblemKind.OTHER_ADJUSTMENTS_DIFFER)
        }
        val everyAdjustmentHasTheSameApprover = linesWithAdjustments.all { lineWithAdjustments ->
            approvedSignatureCountsOf(lineWithAdjustments.otherAdjustments) == firstLineApprovedSignatureCounts
        }
        if (!everyAdjustmentHasTheSameApprover) {
            return AdjustmentsOutcome.Blocked(ConsolidationProblemKind.APPROVERS_DIFFER)
        }

        val carriedOtherAdjustments = firstLineOtherAdjustments.map { adjustment ->
            adjustment.copy(
                identity = SessionIdentity.mintFreshIdentity(),
                relatedSaleLineIdentity = mergedLine.identity,
            )
        }
        val overriddenOutcome = mergedPriceOverride as? MergedPriceOverrideBuilder.PriceOverrideOutcome.Overridden
        val carriedPriceOverride = overriddenOutcome?.saleAdjustment
        return AdjustmentsOutcome.Combined(listOfNotNull(carriedPriceOverride) + carriedOtherAdjustments)
    }

    private fun signatureCountsOf(saleAdjustments: List<SaleSessionAdjustment>): Map<AdjustmentSignature, Int> =
        saleAdjustments.groupingBy { signatureOf(it) }.eachCount()

    private fun approvedSignatureCountsOf(
        saleAdjustments: List<SaleSessionAdjustment>,
    ): Map<ApprovedAdjustmentSignature, Int> = saleAdjustments.groupingBy { approvedSignatureOf(it) }.eachCount()

    private fun signatureOf(saleAdjustment: SaleSessionAdjustment) = AdjustmentSignature(
        adjustmentReasonId = saleAdjustment.adjustmentReasonId,
        direction = saleAdjustment.direction,
        calculationMethod = saleAdjustment.calculationMethod,
        plainValue = saleAdjustment.value.stripTrailingZeros().toPlainString(),
    )

    private fun approvedSignatureOf(saleAdjustment: SaleSessionAdjustment) =
        ApprovedAdjustmentSignature(signatureOf(saleAdjustment), saleAdjustment.approvedById)

    private data class AdjustmentSignature(
        val adjustmentReasonId: UUID,
        val direction: AdjustmentDirection,
        val calculationMethod: PriceAdjustmentMethod,
        val plainValue: String,
    )

    private data class ApprovedAdjustmentSignature(
        val signature: AdjustmentSignature,
        val approvedById: UUID?,
    )
}
