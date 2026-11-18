package app.myliberty.easy_records.organizations.business.retail.unitvalue

import java.time.OffsetDateTime
import java.util.UUID

data class UnitValueDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val referenceNumber: String,
    val name: String? = null,
    val description: String? = null,
    val code: String? = null,
    val unitGroupId: UUID? = null,
    val baseUnit: UUID? = null,
    val unitsOfBasePerUnit: Long? = null,
    val systemDefined: Boolean = false
)
