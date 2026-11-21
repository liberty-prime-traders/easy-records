package app.myliberty.easy_records.locations.business.retail.stock.api.read

import java.util.UUID

data class PurchaseMovementContextCandidate(
    val stockMovementId: UUID,
    val purchaseReferenceNumber: String,
)
