package app.myliberty.easy_records.organizations.business.retail.adjustment_reason

import app.myliberty.easy_records.organizations.business.retail.adjustment_reason.api.AdjustmentDirection
import java.time.OffsetDateTime
import java.util.UUID

data class AdjustmentReasonDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val name: String,
    val code: String? = null,
    val direction: AdjustmentDirection,
    val systemDefined: Boolean
)
