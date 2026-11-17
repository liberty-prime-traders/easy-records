package app.myliberty.easy_records.locations.business.location_product.api

import java.util.UUID

data class LocationProductUnitRequestDto(
    val locationProductId: UUID,
    val unitId: UUID
)
