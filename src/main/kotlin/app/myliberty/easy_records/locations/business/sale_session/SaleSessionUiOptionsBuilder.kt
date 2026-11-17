package me.ezra_home.retail_software_solution.locations.business.sale_session

import me.ezra_home.retail_software_solution.locations.business.sale.api.SaleStatus
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleDiscardAction
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSession
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionAction
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionKind
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionRules
import me.ezra_home.retail_software_solution.locations.business.sale_session.api.SaleSessionUiOptions
import java.util.UUID

object SaleSessionUiOptionsBuilder {

    fun build(
        saleSession: SaleSession,
        inactiveLocationProductIds: Set<UUID>,
        showActiveUserWarning: Boolean,
        canGenerateConsolidatedSale: Boolean,
    ): SaleSessionUiOptions {
        val canSaveDraft = saleSession.allows(SaleSessionAction.SAVE_DRAFT)
        val canConfirm = saleSession.allows(SaleSessionAction.CONFIRM)
        val inactiveProductBlockers = inactiveProductBlockers(saleSession, inactiveLocationProductIds)
        return SaleSessionUiOptions(
            canAddLines = saleSession.allows(SaleSessionAction.ADD_LINE),
            canEditLines = saleSession.allows(SaleSessionAction.EDIT_LINES),
            canChangeContact = saleSession.allows(SaleSessionAction.UPDATE_CONTACT),
            canSaveDraft = canSaveDraft,
            canConfirm = canConfirm,
            saveDraftBlockers = if (canSaveDraft) inactiveProductBlockers else emptyList(),
            confirmBlockers = if (canConfirm) confirmBlockers(saleSession) + inactiveProductBlockers else emptyList(),
            allowedDiscardAction = allowedDiscardAction(saleSession),
            canCopySale = saleSession.saleId != null && SaleSessionRules.isCopyable(saleSession.originalStatus),
            canAddPaymentsToSale = saleSession.canAddPayments(),
            showUnreservedChangesWarning = saleSession.showUnreservedChangesWarning,
            showActiveUserWarning = showActiveUserWarning,
            canGenerateConsolidatedSale = canGenerateConsolidatedSale,
        )
    }

    private fun confirmBlockers(saleSession: SaleSession): List<String> = listOfNotNull(
        "Sale must have at least one line".takeIf { saleSession.saleLines.isEmpty() },
        "Walk-in customers must pay in full".takeIf { saleSession.walkInIsNotFullyCovered() },
        "Total paid cannot exceed order total".takeIf { saleSession.paymentsExceedTotal() },
    )

    private fun inactiveProductBlockers(saleSession: SaleSession, inactiveLocationProductIds: Set<UUID>): List<String> =
        saleSession.saleLines
            .filter { it.locationProductId in inactiveLocationProductIds }
            .map { "${it.productPrintLabel} is no longer active" }
            .distinct()

    private fun allowedDiscardAction(saleSession: SaleSession): SaleDiscardAction? {
        if (!saleSession.allows(SaleSessionAction.VOID_SALE)) return null
        if (saleSession.kind == SaleSessionKind.PENDING_CONSOLIDATION) {
            return SaleDiscardAction.DISCARD_PENDING_CONSOLIDATION
        }
        return when (saleSession.originalStatus) {
            SaleStatus.DRAFT -> SaleDiscardAction.DISCARD_DRAFT
            SaleStatus.CONFIRMED ->
                SaleDiscardAction.VOID_SALE.takeIf { saleSession.salePayments.none { it.voidedReason == null } }
            SaleStatus.VOIDED,
            SaleStatus.DISCARDED -> null
        }
    }
}
