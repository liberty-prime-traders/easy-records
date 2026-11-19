package app.myliberty.easy_records.organizations.business.retail.stock_movement_reason.api

import java.time.OffsetDateTime
import java.util.UUID

data class StockMovementReasonResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val code: String?,
    val name: String,
    val description: String?,
    val movementTypes: Set<MovementType>,
    val systemDefined: Boolean,
    val createdBy: String,
    val createdOn: OffsetDateTime
)
