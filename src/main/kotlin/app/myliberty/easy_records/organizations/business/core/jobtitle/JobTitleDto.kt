package app.myliberty.easy_records.organizations.business.jobtitle

import java.time.OffsetDateTime
import java.util.UUID

data class JobTitleDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val value: String? = null
)
