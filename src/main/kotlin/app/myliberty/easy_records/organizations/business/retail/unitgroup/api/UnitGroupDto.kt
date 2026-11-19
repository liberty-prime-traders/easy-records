package app.myliberty.easy_records.organizations.business.retail.unitgroup.api

import java.time.OffsetDateTime
import java.util.UUID

data class UnitGroupDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val code: String? = null,
    val name: String,
    val description: String? = null,
    val systemDefined: Boolean = false
)
