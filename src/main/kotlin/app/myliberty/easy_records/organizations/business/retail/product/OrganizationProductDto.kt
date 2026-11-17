package app.myliberty.easy_records.organizations.business.retail.product

import app.myliberty.easy_records.organizations.business.retail.product.api.ProductStatus
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationProductDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val productName: String,
    val description: String? = null,
    val productGroupId: UUID,
    val productGroupName: String,
    val baseUnitId: UUID,
    val status: ProductStatus
)
