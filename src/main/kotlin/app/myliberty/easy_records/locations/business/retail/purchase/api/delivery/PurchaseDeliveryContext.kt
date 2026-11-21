package app.myliberty.easy_records.locations.business.retail.purchase.api.delivery

import java.util.UUID
import app.myliberty.easy_records.locations.business.retail.purchase.api.PurchaseLineDto

data class PurchaseDeliveryContext(
    val purchaseId: UUID,
    val purchaseReferenceNumber: String,
    val supplierId: UUID,
    val purchaseLineById: Map<UUID, PurchaseLineDto>
)
