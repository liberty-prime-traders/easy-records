package app.myliberty.easy_records.organizations.business.core.tag.api

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class TagResponseDto(
    val id: UUID,
    val category: CategoryType?,
    val tagName: String?,
    val description: String?,
    val createdBy: String,
    val createdOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
