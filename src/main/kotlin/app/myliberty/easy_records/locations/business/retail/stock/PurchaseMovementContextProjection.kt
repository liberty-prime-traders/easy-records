package app.myliberty.easy_records.locations.business.retail.stock

import java.util.UUID

interface PurchaseMovementContextProjection {
    val stockMovementId: UUID
    val purchaseReferenceNumber: String
}
