package app.myliberty.easy_records.locations.business.stock

import java.util.UUID

interface PurchaseMovementContextProjection {
    val stockMovementId: UUID
    val purchaseReferenceNumber: String
}
