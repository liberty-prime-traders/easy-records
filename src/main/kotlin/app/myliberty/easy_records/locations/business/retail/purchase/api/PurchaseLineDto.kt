package app.myliberty.easy_records.locations.business.retail.purchase.api

import app.myliberty.easy_records.util.quantity.ExactQuantity
import java.math.BigDecimal
import java.util.UUID

data class PurchaseLineDto(
    val id: UUID,
    val purchaseId: UUID,
    val locationProductId: UUID,
    var unitCost: BigDecimal,
    val storageUnitsPerUserProvidedUnit: Long,
    val unitId: UUID,
    val storageNetOrderedQty: ExactQuantity,
    val storagePendingDeliveryQty: ExactQuantity
)
