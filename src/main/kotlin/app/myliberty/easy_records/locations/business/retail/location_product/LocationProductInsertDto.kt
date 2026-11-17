package app.myliberty.easy_records.locations.business.location_product

import app.myliberty.easy_records.organizations.business.product.api.ProductStatus
import java.time.OffsetDateTime
import java.util.UUID

data class LocationProductInsertDto(
    val orgProductId: UUID,
    val productName: String,
    val description: String? = null,
    val productGroupName: String,
    val categoryId: UUID,
    val baseUnitId: UUID,
    val status: ProductStatus = ProductStatus.ACTIVE,
    val lastSyncedAt: OffsetDateTime? = null
)
