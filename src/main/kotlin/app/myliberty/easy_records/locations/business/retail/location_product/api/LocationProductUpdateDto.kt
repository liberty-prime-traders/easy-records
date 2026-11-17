package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.locations.business.retail.location_product.LocationProductDto
import app.myliberty.easy_records.util.business.Money
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class LocationProductUpdateDto(
  val id: UUID,
  val defaultSalePrice: Optional<Money>? = null,
  val minStockLevel: Optional<Int>? = null
) : Serializable {

    fun applyTo(existing: LocationProductDto): LocationProductDto = existing.copy(
        defaultSalePrice = if (defaultSalePrice != null) defaultSalePrice.orElse(null)?.toBigDecimal() else existing.defaultSalePrice,
        minStockLevel = if (minStockLevel != null) minStockLevel.orElse(null) else existing.minStockLevel
    )
}
