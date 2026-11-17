package app.myliberty.easy_records.organizations.business.stock_movement_reason

import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import java.time.OffsetDateTime
import java.util.UUID

data class StockMovementReasonDto(
    val id: UUID,
    val referenceNumber: String,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val code: String? = null,
    val name: String,
    val description: String?,
    val movementTypes: Set<MovementType>,
    val systemDefined: Boolean
)
