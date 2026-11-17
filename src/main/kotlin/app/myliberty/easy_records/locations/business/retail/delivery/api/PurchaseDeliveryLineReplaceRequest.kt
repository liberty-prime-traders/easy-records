package app.myliberty.easy_records.locations.business.delivery.api

import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.quantity.UserProvidedQuantity
import java.util.UUID

data class PurchaseDeliveryLineReplaceRequest(
    val revisedDeliveryLineId: UUID,
    val userProvidedQuantityDelivered: UserProvidedQuantity,
    val unitId: UUID,
    val unitCost: Money,
)
