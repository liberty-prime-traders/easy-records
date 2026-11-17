package app.myliberty.easy_records.locations.business.stock.api.revision

import java.util.UUID

data class StockEntryResetPlan(
    val stockEntryId: UUID,
    val locationProductId: UUID,
    val movementDrafts: List<StockMovementDraft>,
)
