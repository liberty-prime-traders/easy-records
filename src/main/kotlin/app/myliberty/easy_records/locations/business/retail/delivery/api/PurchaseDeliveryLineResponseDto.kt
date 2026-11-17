package app.myliberty.easy_records.locations.business.retail.delivery.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import app.myliberty.easy_records.configuration.serializer.StripZeroesAndRound
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductSummaryDto
import app.myliberty.easy_records.locations.business.retail.purchase.api.delivery.PurchaseDeliveryLineStatus
import app.myliberty.easy_records.util.business.Decimals
import java.math.BigDecimal
import java.util.UUID

data class PurchaseDeliveryLineResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val purchaseLineId: UUID,
  val locationProduct: LocationProductSummaryDto,
  @get:JsonProperty("quantityDelivered")
  val userProvidedQuantityDelivered: BigDecimal,
  val unitId: UUID,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val unitCost: BigDecimal,
  val status: PurchaseDeliveryLineStatus,
) {
  val lineTotal: BigDecimal = Decimals.MoneyScale.multiply(userProvidedQuantityDelivered, unitCost)
}
