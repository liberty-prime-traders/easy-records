package app.myliberty.easy_records.locations.business.retail.purchase_movement_context_backfill

import java.util.UUID

data class PurchaseMovementContextBackfillResult(
    val locationId: UUID,
    val locationName: String,
    val counts: PurchaseMovementContextBackfillCounts,
) {
    val isComplete: Boolean = counts.movementsStillMissingContextCount == 0L
}
