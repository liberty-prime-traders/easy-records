package app.myliberty.easy_records.locations.business.retail.purchase.api

import com.fasterxml.jackson.annotation.JsonProperty
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.quantity.UserProvidedQuantity
import java.util.UUID

data class PurchaseLineUpdateDto(
  val id: UUID,
  @param:JsonProperty("quantityOrdered")
  val userProvidedQuantityOrdered: UserProvidedQuantity,
  val unitId: UUID,
  val unitCost: Money
)
