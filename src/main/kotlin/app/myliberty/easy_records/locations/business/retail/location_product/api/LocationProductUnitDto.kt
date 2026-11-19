package app.myliberty.easy_records.locations.business.retail.location_product.api

import java.util.UUID

data class LocationProductUnitRequestDto(
    val locationProductId: UUID,
    val unitId: UUID
)
