package app.myliberty.easy_records.organizations.business.stock_movement_reason.api

import app.myliberty.easy_records.organizations.business.stock_movement_reason.StockMovementReasonDto
import app.myliberty.easy_records.util.business.StringUtils
import java.util.Optional
import java.util.UUID

data class StockMovementReasonUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null,
    val movementTypes: Set<MovementType>? = null
) {
    fun applyTo(existing: StockMovementReasonDto): StockMovementReasonDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = StringUtils.useIfProvided(description, existing.description),
        movementTypes = movementTypes ?: existing.movementTypes
    )
}
