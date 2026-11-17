package app.myliberty.easy_records.locations.business.retail.sale_session

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleConsolidationEligibility
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductService
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleHeaderDto
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.locations.business.retail.sale_payment.api.SalePaymentValidator
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSession
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAdjustment
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionAction
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionRules
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionLine
import app.myliberty.easy_records.locations.business.retail.sale_session.api.SaleSessionTotals
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentReasonService
import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.SystemAdjustmentReason
import app.myliberty.easy_records.util.business.Decimals
import app.myliberty.easy_records.util.business.DisplayFormatters
import app.myliberty.easy_records.util.exceptions.LibertyGenericException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SaleSessionValidator(
    private val adjustmentReasonService: AdjustmentReasonService,
    private val locationProductService: LocationProductService,
) {

    fun validate(saleSession: SaleSession) {
        if (!SaleSessionRules.allowsDuplicateProducts(saleSession.kind)) {
            guardNoDuplicateProducts(saleSession.saleLines)
        }
        guardOrderLevelAdjustmentsAllowed(saleSession)
        guardAdjustmentReferences(saleSession)
        guardAdjustmentReasons(saleSession.saleAdjustments)
        guardLineOverrideExclusivity(saleSession)
        val totals = SaleSessionTotalsCalculator.compute(
            saleSession.saleLines, saleSession.saleAdjustments, saleSession.totalPaid()
        )
        guardAmountsWithinMaximum(saleSession, totals)
        guardDiscountCeilings(saleSession, totals)
    }

    private fun guardOrderLevelAdjustmentsAllowed(saleSession: SaleSession) {
        val refusal = SaleSessionRules.refusalOrNull(saleSession.kind, SaleSessionAction.ADD_ORDER_LEVEL_ADJUSTMENT)
            ?: return
        if (saleSession.saleAdjustments.any { it.relatedSaleLineIdentity == null }) throw LibertyGenericException(refusal)
    }

    private fun guardAmountsWithinMaximum(saleSession: SaleSession, totals: SaleSessionTotals) {
        saleSession.saleLines.forEach {
            Decimals.requireWithinMaximum(it.lineTotal, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Line total of ${it.productLabel}")
        }
        saleSession.saleAdjustments.forEach {
            val adjustmentAmount = SaleSessionTotalsCalculator.calculateAdjustmentAmount(it, saleSession.saleLines)
            Decimals.requireWithinMaximum(adjustmentAmount, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Adjustment amount")
        }
        Decimals.requireWithinMaximum(totals.subtotal, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Sale subtotal")
        Decimals.requireWithinMaximum(totals.receivableTotal, Decimals.MAXIMUM_TRANSACTION_AMOUNT, "Sale total")
    }

    private fun guardLineOverrideExclusivity(saleSession: SaleSession) {
        val priceOverrideReasonId = adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)
        val lineKeysWithOverride = saleSession.saleAdjustments
            .filter { it.isPriceOverride(priceOverrideReasonId) }
            .mapTo(HashSet()) { it.requiredRelatedSaleLineKey() }
        if (lineKeysWithOverride.isEmpty()) return
        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }
        saleSession.saleAdjustments.forEach { saleSessionAdjustment ->
            val targetLineKey = saleSessionAdjustment.relatedSaleLineIdentity?.key() ?: return@forEach
            if (saleSessionAdjustment.adjustmentReasonId == priceOverrideReasonId) return@forEach
            if (targetLineKey !in lineKeysWithOverride) return@forEach
            val productLabel = saleSessionLinesByKey[targetLineKey]?.productLabel ?: "this line"
            throw LibertyGenericException(
                "$productLabel has both a price override and another adjustment — " +
                " Remove the override and use adjustments to update price"
            )
        }
    }

    fun guardReasonIsNotPriceOverride(adjustmentReasonId: UUID) {
        if (adjustmentReasonId == adjustmentReasonService.getSystemReasonId(SystemAdjustmentReason.PRICE_OVERRIDE)) {
            throw LibertyGenericException("The Price Override reason is applied by changing a line's price, not by adding an adjustment")
        }
    }

    fun guardAllowed(saleSession: SaleSession, action: SaleSessionAction) {
        if (saleSession.allows(action)) return
        throw LibertyGenericException(
            SaleSessionRules.refusalOrNull(saleSession.kind, action)
                ?: "Sale session is read-only because the underlying sale is ${saleSession.originalStatus}"
        )
    }

    fun canAddPayments(saleSession: SaleSession) {
        guardAllowed(saleSession, SaleSessionAction.ADD_PAYMENT)
        if (!saleSession.canAddPayments()) {
            throw LibertyGenericException(
                "Cannot add a payment because the sale is ${saleSession.originalStatus}"
            )
        }
    }

    fun guardSourceSaleCopyable(sourceSaleStatus: SaleStatus) {
        if (!SaleSessionRules.isCopyable(sourceSaleStatus)) {
            throw LibertyGenericException("A confirmed sale cannot be copied")
        }
    }

    fun guardConsolidationSelectionSize(selectedSaleCount: Int) {
        if (selectedSaleCount !in MINIMUM_SALES_TO_CONSOLIDATE..MAXIMUM_SALES_TO_CONSOLIDATE) {
            throw LibertyGenericException(
                "Select between $MINIMUM_SALES_TO_CONSOLIDATE and $MAXIMUM_SALES_TO_CONSOLIDATE sales to consolidate"
            )
        }
    }

    fun guardSingleContactAcrossSales(sourceSaleHeaders: List<SaleHeaderDto>) {
        val distinctContactIds = sourceSaleHeaders.map { it.contactId }.toSet()
        if (distinctContactIds.size > 1) {
            throw LibertyGenericException("Sales for different customers cannot be consolidated")
        }
    }

    fun guardSourceSalesConsolidatable(sourceSaleHeaders: List<SaleHeaderDto>) {
        sourceSaleHeaders.firstOrNull { !SaleSessionRules.isConsolidatable(it.status) }
            ?.let { saleHeader ->
                throw LibertyGenericException(
                    "Sale ${saleHeader.referenceNumber} is ${saleHeader.status.name.lowercase()}; " +
                        "only draft and confirmed sales can be consolidated"
                )
            }
        if (sourceSaleHeaders.any { SaleConsolidationEligibility.isWalkInSale(it.contactId) }) {
            throw LibertyGenericException("Walk-in sales cannot be consolidated")
        }
    }

    fun guardNoInactiveProducts(saleSession: SaleSession) {
        locationProductService.guardAllActive(saleSession.saleLines.map { it.locationProductId })
    }

    fun guardNonEmptyLines(saleSession: SaleSession) {
        if (saleSession.saleLines.isEmpty()) {
            throw LibertyGenericException("Sale must have at least one line")
        }
    }

    fun guardWalkInFullyCovered(saleSession: SaleSession) {
        if (saleSession.walkInIsNotFullyCovered()) {
            throw LibertyGenericException("Walk-in sales require full payment coverage")
        }
    }

    fun guardPaymentsWithinTotal(saleSession: SaleSession) {
        if (!saleSession.paymentsExceedTotal()) return
        SalePaymentValidator.guardNotExceedingSaleTotal(
            totalSubmitted = saleSession.totals.paymentTotal,
            receivableTotal = saleSession.totals.receivableTotal,
        )
    }

    private fun guardNoDuplicateProducts(saleSessionLines: List<SaleSessionLine>) {
        val locationProductIds = saleSessionLines.map { it.locationProductId }
        if (locationProductIds.size != locationProductIds.toSet().size) {
            throw LibertyGenericException("Duplicate products are not allowed in a sale")
        }
    }

    private fun guardAdjustmentReferences(saleSession: SaleSession) {
        val saleSessionLineKeys = saleSession.saleLines.mapTo(HashSet()) { it.identity.key() }
        saleSession.saleAdjustments.forEach { saleSessionAdjustment ->
            val targetLineKey = saleSessionAdjustment.relatedSaleLineIdentity?.key() ?: return@forEach
            if (targetLineKey !in saleSessionLineKeys) {
                throw LibertyGenericException("Adjustment references a line that is not on the sale")
            }
        }
    }

    private fun guardAdjustmentReasons(saleSessionAdjustments: List<SaleSessionAdjustment>) {
        saleSessionAdjustments.forEach { saleSessionAdjustment ->
            adjustmentReasonService.requireCanApply(
                saleSessionAdjustment.adjustmentReasonId,
                saleSessionAdjustment.direction,
            )
        }
    }

    private fun guardDiscountCeilings(saleSession: SaleSession, totals: SaleSessionTotals) {
        guardLineDiscountCeilings(saleSession)
        val remainingSubtotalAfterLineDiscounts = totals.subtotal - totals.lineLevelDiscountTotal
        if (totals.orderLevelDiscountTotal > remainingSubtotalAfterLineDiscounts) {
            throw LibertyGenericException(
                "Order-level discount total of ${DisplayFormatters.formatCurrency(totals.orderLevelDiscountTotal)} exceeds " +
                        "remaining subtotal ${DisplayFormatters.formatCurrency(remainingSubtotalAfterLineDiscounts)} " +
                        "(subtotal ${DisplayFormatters.formatCurrency(totals.subtotal)} " +
                        "minus line discounts ${DisplayFormatters.formatCurrency(totals.lineLevelDiscountTotal)})."
            )
        }
    }

    private fun guardLineDiscountCeilings(saleSession: SaleSession) {
        val saleSessionLinesByKey = saleSession.saleLines.associateBy { it.identity.key() }
        val lineDiscountsByLineKey = saleSession.saleAdjustments
            .filter { it.direction == AdjustmentDirection.DISCOUNT && it.relatedSaleLineIdentity != null }
            .groupBy { it.requiredRelatedSaleLineKey() }
        lineDiscountsByLineKey.forEach { (targetLineKey, lineDiscounts) ->
            val targetSaleSessionLine = saleSessionLinesByKey[targetLineKey]
                ?: throw LibertyGenericException("Adjustment references a line that is not on the sale")
            val totalDiscountAmount = lineDiscounts.sumOf {
                SaleSessionTotalsCalculator.calculateAdjustmentAmount(it, saleSession.saleLines)
            }
            val lineTotal = targetSaleSessionLine.lineTotal
            if (totalDiscountAmount > lineTotal) {
                throw LibertyGenericException(
                    "On ${targetSaleSessionLine.productLabel}, total discounts of " +
                            "${DisplayFormatters.formatCurrency(totalDiscountAmount)} exceed " +
                            "line total of ${DisplayFormatters.formatCurrency(lineTotal)}."
                )
            }
        }
    }

    private companion object {
        const val MINIMUM_SALES_TO_CONSOLIDATE = 2
        const val MAXIMUM_SALES_TO_CONSOLIDATE = 20
    }
}
