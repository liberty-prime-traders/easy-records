package app.myliberty.easy_records.locations.business.retail.purchase.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import app.myliberty.easy_records.configuration.serializer.StripZeroesAndRound
import app.myliberty.easy_records.locations.business.retail.location_product.api.LocationProductSummaryDto
import java.io.Serializable
import java.math.BigDecimal
import java.util.UUID

// quantity* fields are display-only in the ordered unit and can lose digits; storageQuantity* fields and breakdown texts are exact
data class PurchaseLineResponseDto(
  val id: UUID,
  val referenceNumber: String,
  val locationProduct: LocationProductSummaryDto,
  @get:JsonProperty("quantityOrdered")
  val userProvidedQuantityOrdered: BigDecimal,
  val unitId: UUID,
  val conversionFactor: BigDecimal,
  val unitCost: BigDecimal,
  val lineTotal: BigDecimal,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val quantityDelivered: BigDecimal,
  @field:JsonSerialize(using = StripZeroesAndRound::class)
  val quantityYetToBeDelivered: BigDecimal,
  val quantityCanceled: BigDecimal,
  val quantityExpected: BigDecimal,
  val storageQuantityDelivered: BigDecimal,
  val storageQuantityYetToBeDelivered: BigDecimal,
  val storageQuantityCanceled: BigDecimal,
  val storageQuantityExpected: BigDecimal,
  val quantityDeliveredBreakdown: String,
  val quantityYetToBeDeliveredBreakdown: String,
  val quantityCanceledBreakdown: String,
  val quantityExpectedBreakdown: String,
) : Serializable
