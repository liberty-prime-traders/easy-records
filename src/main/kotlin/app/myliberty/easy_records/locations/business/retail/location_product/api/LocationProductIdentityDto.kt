package app.myliberty.easy_records.locations.business.retail.location_product.api

import java.util.UUID

data class LocationProductIdentityDto(
    val locationProductId: UUID,
    val orgProductId: UUID
)
