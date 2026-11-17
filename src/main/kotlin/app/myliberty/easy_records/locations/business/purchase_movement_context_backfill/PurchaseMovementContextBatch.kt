package app.myliberty.easy_records.locations.business.purchase_movement_context_backfill

import java.util.UUID

data class PurchaseMovementContextBatch(
    val updatedMovementCount: Int,
    val lastStockMovementId: UUID,
)
