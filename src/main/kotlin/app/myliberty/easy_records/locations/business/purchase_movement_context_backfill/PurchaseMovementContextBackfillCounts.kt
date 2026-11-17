package app.myliberty.easy_records.locations.business.purchase_movement_context_backfill

data class PurchaseMovementContextBackfillCounts(
    val updatedMovementCount: Long,
    val movementsStillMissingContextCount: Long,
)
