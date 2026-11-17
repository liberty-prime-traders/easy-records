package app.myliberty.easy_records.locations.business.retail.stock_revision.api

import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.quantity.UserProvidedQuantity
import java.util.UUID

data class DeliveryLineRevisionRequest(
    val revisedDeliveryLineId: UUID,
    val userProvidedQuantityDelivered: UserProvidedQuantity,
    val unitId: UUID,
    val unitCost: Money,
    val reason: String,
)
