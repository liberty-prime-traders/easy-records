package app.myliberty.easy_records.locations.business.retail.location_product.api

import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import java.util.UUID

data class LocationProductSyncDto(
    val orgProductId: UUID,
    val productName: String,
    val description: String?,
    val productGroupName: String,
    val status: ProductStatus,
    val baseUnitId: UUID,
    val categoryId: UUID
)
