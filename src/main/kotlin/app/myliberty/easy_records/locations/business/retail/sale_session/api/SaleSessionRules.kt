package app.myliberty.easy_records.locations.business.retail.sale_session.api

import app.myliberty.easy_records.locations.business.retail.sale.api.SaleConsolidationEligibility
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus

object SaleSessionRules {

    fun refusalOrNull(kind: SaleSessionKind, action: SaleSessionAction): String? = when (kind) {
        SaleSessionKind.SALE -> saleRefusalOrNull(action)
        SaleSessionKind.PENDING_CONSOLIDATION -> pendingConsolidationRefusalOrNull(action)
    }

    fun allows(kind: SaleSessionKind, action: SaleSessionAction): Boolean = refusalOrNull(kind, action) == null

    // A pending consolidation holds unmerged lines of problem products, so one product on several lines is expected.
    fun allowsDuplicateProducts(kind: SaleSessionKind): Boolean = when (kind) {
        SaleSessionKind.SALE -> false
        SaleSessionKind.PENDING_CONSOLIDATION -> true
    }

    fun isCopyable(sourceSaleStatus: SaleStatus): Boolean = sourceSaleStatus != SaleStatus.CONFIRMED

    fun isConsolidatable(sourceSaleStatus: SaleStatus): Boolean =
        SaleConsolidationEligibility.statusAllows(sourceSaleStatus)

    fun reservesStock(kind: SaleSessionKind): Boolean = when (kind) {
        SaleSessionKind.SALE -> true
        SaleSessionKind.PENDING_CONSOLIDATION -> false
    }

    fun keepsRemovedLines(kind: SaleSessionKind): Boolean = when (kind) {
        SaleSessionKind.SALE -> false
        SaleSessionKind.PENDING_CONSOLIDATION -> true
    }

    private fun saleRefusalOrNull(action: SaleSessionAction): String? = when (action) {
        SaleSessionAction.RESTORE_REMOVED_LINE -> "Only a pending consolidation keeps removed lines"
        SaleSessionAction.GENERATE_CONSOLIDATED_SALE -> "Only a pending consolidation can be generated"
        SaleSessionAction.UPDATE_CONTACT,
        SaleSessionAction.UPDATE_SOLD_BY,
        SaleSessionAction.UPDATE_DATE_SOLD,
        SaleSessionAction.UPDATE_NOTES,
        SaleSessionAction.ADD_LINE,
        SaleSessionAction.EDIT_LINES,
        SaleSessionAction.ADD_LINE_ADJUSTMENT,
        SaleSessionAction.ADD_ORDER_LEVEL_ADJUSTMENT,
        SaleSessionAction.REMOVE_ADJUSTMENT,
        SaleSessionAction.SAVE_DRAFT,
        SaleSessionAction.CONFIRM,
        SaleSessionAction.ADD_PAYMENT,
        SaleSessionAction.REMOVE_PAYMENT,
        SaleSessionAction.VOID_SALE -> null
    }

    private fun pendingConsolidationRefusalOrNull(action: SaleSessionAction): String? = when (action) {
        SaleSessionAction.UPDATE_CONTACT -> "The contact is the contact of the sales being combined"
        SaleSessionAction.UPDATE_SOLD_BY,
        SaleSessionAction.UPDATE_DATE_SOLD -> "Set this on the combined sale after it is generated"
        SaleSessionAction.ADD_ORDER_LEVEL_ADJUSTMENT ->
            "Order-level discounts and surcharges are added after the combined sale is generated"
        SaleSessionAction.ADD_PAYMENT,
        SaleSessionAction.REMOVE_PAYMENT -> "Payments are taken after the combined sale is generated"
        SaleSessionAction.SAVE_DRAFT,
        SaleSessionAction.CONFIRM ->
            "This session only holds the lines that could not be combined, so it cannot be saved or confirmed"
        SaleSessionAction.ADD_LINE ->
            "Add the product to the combined sale after it is generated; here, fix or remove the lines shown"
        SaleSessionAction.UPDATE_NOTES,
        SaleSessionAction.EDIT_LINES,
        SaleSessionAction.RESTORE_REMOVED_LINE,
        SaleSessionAction.GENERATE_CONSOLIDATED_SALE,
        SaleSessionAction.ADD_LINE_ADJUSTMENT,
        SaleSessionAction.REMOVE_ADJUSTMENT,
        SaleSessionAction.VOID_SALE -> null
    }
}
