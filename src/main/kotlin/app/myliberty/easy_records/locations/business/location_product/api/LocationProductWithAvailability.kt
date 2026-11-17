package app.myliberty.easy_records.locations.business.location_product.api

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import app.myliberty.easy_records.configuration.serializer.StripZeroesAndRound
import java.math.BigDecimal
import java.util.UUID

data class LocationProductWithAvailability(
    val id: UUID,
    val referenceNumber: String,
    val productName: String,
    val productGroupName: String,
    val quantityOnHand: BigDecimal,
    val quantityReserved: BigDecimal,
    val quantityAvailable: BigDecimal,
    @field:JsonSerialize(using = StripZeroesAndRound::class)
    val defaultSalePrice: BigDecimal,
    val storageQuantityOnHand: BigDecimal,
    val storageQuantityReserved: BigDecimal,
    val storageQuantityAvailable: BigDecimal,
    val quantityOnHandBreakdown: String,
    val quantityAvailableBreakdown: String,
)
