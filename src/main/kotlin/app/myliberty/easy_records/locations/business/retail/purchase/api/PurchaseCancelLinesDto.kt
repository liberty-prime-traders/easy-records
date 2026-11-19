package app.myliberty.easy_records.locations.business.retail.purchase.api

import com.fasterxml.jackson.annotation.JsonProperty
import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

// unitId is not stored; absent means the line's ordered unit
data class PurchaseCancelLinesDto(
  val purchaseLineId: UUID,
  @param:JsonProperty("quantityCanceled")
  val userProvidedQuantityCanceled: PositiveUserProvidedQuantity,
  val unitId: UUID? = null
)
