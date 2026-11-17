package app.myliberty.easy_records.organizations.business.retail.product_tag

import java.time.OffsetDateTime
import java.util.UUID

data class ProductTagDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val endOn: OffsetDateTime? = null,
    val orgProductId: UUID,
    val tagId: UUID
)
