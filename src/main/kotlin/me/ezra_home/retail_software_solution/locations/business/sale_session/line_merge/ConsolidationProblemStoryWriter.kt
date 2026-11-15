package me.ezra_home.retail_software_solution.locations.business.sale_session.line_merge

import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionLine
import me.ezra_home.retail_software_solution.util.business.DisplayFormatters
import me.ezra_home.retail_software_solution.util.exceptions.BlockingProblem
import me.ezra_home.retail_software_solution.util.quantity.QuantityLimits
import java.util.UUID

object ConsolidationProblemStoryWriter {

    fun write(
        consolidationProblems: List<ConsolidationProblem>,
        problemSaleLines: List<SaleSessionLine>,
        sourceSaleReferenceByLineKey: Map<UUID, String>,
        unitNameByUnitId: Map<UUID, String>,
    ): List<BlockingProblem> {
        val problemSaleLineByLineKey = problemSaleLines.associateBy { it.lineKey }
        return consolidationProblems.map { consolidationProblem ->
            val saleLines = consolidationProblem.saleLineIdentities.map { problemSaleLineByLineKey.getValue(it.key()) }
            val sourceSaleReferences = saleLines
                .map { sourceSaleReferenceByLineKey.getValue(it.lineKey) }
                .distinct()
            val productLabel = saleLines.first().productLabel
            BlockingProblem(
                documentReference = sourceSaleReferences.joinToString(", "),
                description = description(consolidationProblem.kind, productLabel),
                effect = effect(
                    consolidationProblem.kind,
                    linesText(saleLines, sourceSaleReferenceByLineKey, unitNameByUnitId)
                ),
            )
        }.sortedWith(compareBy({ it.documentReference }, { it.description }))
    }

    private fun description(kind: ConsolidationProblemKind, productLabel: String): String = when (kind) {
        ConsolidationProblemKind.INACTIVE_PRODUCT ->
            "$productLabel is no longer active, so it cannot go on a new sale."
        ConsolidationProblemKind.DIFFERENT_LIST_PRICES ->
            "$productLabel was sold at different list prices, so its lines cannot be added into one."
        ConsolidationProblemKind.UNITS_NOT_EXACT ->
            "$productLabel was sold in units that cannot be added together without losing part of a quantity."
        ConsolidationProblemKind.QUANTITY_TOO_PRECISE ->
            "Adding up $productLabel would need more than ${QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES} " +
                "decimal places."
        ConsolidationProblemKind.QUANTITY_TOO_LARGE ->
            "Adding up $productLabel would go above the largest allowed quantity of " +
                "${DisplayFormatters.formatQuantity(QuantityLimits.MAX_USER_QUANTITY)}."
        ConsolidationProblemKind.OVERRIDE_ON_SOME_LINES_ONLY ->
            "$productLabel has a typed price on some of its lines but not on others."
        ConsolidationProblemKind.TYPED_PRICES_DIFFER ->
            "$productLabel has different typed prices on its lines."
        ConsolidationProblemKind.TYPED_PRICE_NOT_TYPEABLE ->
            "The typed price of $productLabel cannot be carried onto the combined line exactly."
        ConsolidationProblemKind.APPROVERS_DIFFER ->
            "The price changes on $productLabel were approved by different people."
        ConsolidationProblemKind.OTHER_ADJUSTMENTS_DIFFER ->
            "$productLabel has discounts or surcharges that differ between its lines."
        ConsolidationProblemKind.TOTAL_DRIFT ->
            "One combined line of $productLabel would not add up to the same total as its separate lines."
    }

    private fun effect(kind: ConsolidationProblemKind, linesText: String): String {
        val fix = when (kind) {
            ConsolidationProblemKind.INACTIVE_PRODUCT ->
                "Remove these lines, or reactivate the product and start again."
            ConsolidationProblemKind.DIFFERENT_LIST_PRICES ->
                "Keep one of these lines and remove the others, then add the quantity back on the kept line."
            ConsolidationProblemKind.UNITS_NOT_EXACT ->
                "Change one of these lines to a unit the others can be counted in, or replace them with one line."
            ConsolidationProblemKind.QUANTITY_TOO_PRECISE ->
                "Change the quantities so that they add up to ${QuantityLimits.MAX_USER_QUANTITY_DECIMAL_PLACES} " +
                    "decimal places or fewer, or replace these lines with one line."
            ConsolidationProblemKind.QUANTITY_TOO_LARGE ->
                "Reduce the quantities, or replace these lines with smaller ones."
            ConsolidationProblemKind.OVERRIDE_ON_SOME_LINES_ONLY ->
                "Give every one of these lines the same typed price, or clear it from all of them."
            ConsolidationProblemKind.TYPED_PRICES_DIFFER ->
                "Type the same unit price on every one of these lines, or replace them with one line."
            ConsolidationProblemKind.TYPED_PRICE_NOT_TYPEABLE ->
                "Clear the typed price from these lines, or replace them with one line priced as you want it."
            ConsolidationProblemKind.APPROVERS_DIFFER ->
                "Have one person approve the price on every one of these lines, or replace them with one line."
            ConsolidationProblemKind.OTHER_ADJUSTMENTS_DIFFER ->
                "Make the discounts and surcharges identical on every one of these lines, or replace them with " +
                    "one line."
            ConsolidationProblemKind.TOTAL_DRIFT ->
                "Edit the price or the quantity on these lines until one combined line gives the same total, " +
                    "or replace them with one line."
        }
        return "Lines: $linesText. $fix"
    }

    private fun linesText(
        saleLines: List<SaleSessionLine>,
        sourceSaleReferenceByLineKey: Map<UUID, String>,
        unitNameByUnitId: Map<UUID, String>,
    ): String = saleLines.joinToString("; ") { saleLine ->
        val quantityText = DisplayFormatters.formatQuantity(saleLine.userProvidedQuantity)
        val unitName = unitNameByUnitId.getValue(saleLine.unitId)
        val listPriceText = DisplayFormatters.formatCurrency(saleLine.defaultSalePrice)
        "$quantityText $unitName at $listPriceText from ${sourceSaleReferenceByLineKey.getValue(saleLine.lineKey)}"
    }
}
