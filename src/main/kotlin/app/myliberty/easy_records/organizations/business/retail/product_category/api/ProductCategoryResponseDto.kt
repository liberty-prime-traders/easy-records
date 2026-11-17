package app.myliberty.easy_records.organizations.business.retail.product_category.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class ProductCategoryResponseDto(
    val id: UUID,
    val categoryName: String?,
    val description: String?,
    val createdBy: String,
    val createdOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
