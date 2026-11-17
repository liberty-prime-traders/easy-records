package app.myliberty.easy_records.locations.business.delivery.api

import com.fasterxml.jackson.annotation.JsonProperty
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class PurchaseDeliveryLineCreateDto(
  val purchaseLineId: UUID,
  @param:JsonProperty("quantityDelivered")
  val userProvidedQuantityDelivered: PositiveUserProvidedQuantity,
  val unitId: UUID,
  val unitCost: PositiveMoney
)
