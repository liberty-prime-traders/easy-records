package app.myliberty.easy_records.organizations.business.core.tag.api

import java.time.OffsetDateTime
import java.util.UUID

data class TagDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val category: CategoryType,
    val tagName: String,
    val description: String? = null
)
