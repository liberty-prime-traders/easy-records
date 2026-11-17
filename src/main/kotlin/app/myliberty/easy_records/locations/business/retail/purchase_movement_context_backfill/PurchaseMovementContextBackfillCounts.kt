package app.myliberty.easy_records.locations.business.retail.purchase_movement_context_backfill

data class PurchaseMovementContextBackfillCounts(
    val updatedMovementCount: Long,
    val movementsStillMissingContextCount: Long,
)
