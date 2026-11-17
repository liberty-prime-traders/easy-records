package app.myliberty.easy_records.locations.business.purchase.api

import com.fasterxml.jackson.annotation.JsonProperty
import app.myliberty.easy_records.locations.business.purchase.HasLocationProduct
import app.myliberty.easy_records.util.business.Money
import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class PurchaseLineCreateDto(
  override val locationProductId: UUID,
  @param:JsonProperty("quantityOrdered")
  val userProvidedQuantityOrdered: PositiveUserProvidedQuantity,
  val unitId: UUID,
  val unitCost: Money
) : HasLocationProduct
