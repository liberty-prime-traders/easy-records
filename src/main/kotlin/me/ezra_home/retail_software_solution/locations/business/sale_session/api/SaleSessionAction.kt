package me.ezra_home.retail_software_solution.locations.business.sale_session.api

enum class SaleSessionAction {
    UPDATE_CONTACT,
    UPDATE_SOLD_BY,
    UPDATE_DATE_SOLD,
    UPDATE_NOTES,
    ADD_LINE,
    EDIT_LINES,
    RESTORE_REMOVED_LINE,
    ADD_LINE_ADJUSTMENT,
    ADD_ORDER_LEVEL_ADJUSTMENT,
    REMOVE_ADJUSTMENT,
    SAVE_DRAFT,
    CONFIRM,
    GENERATE_CONSOLIDATED_SALE,
    ADD_PAYMENT,
    REMOVE_PAYMENT,
    VOID_SALE;

    // Payments, voids and notes stay open on a confirmed, voided or discarded sale, so they skip the draft check;
    // every other action needs it.
    fun needsMutableSale(): Boolean = this !in actionsWithTheirOwnStatusRule

    companion object {
        private val actionsWithTheirOwnStatusRule = setOf(ADD_PAYMENT, REMOVE_PAYMENT, VOID_SALE, UPDATE_NOTES)
    }
}
