package app.myliberty.easy_records.organizations.business.stock_movement_reason.api


data class StockMovementReasonInsertDto(
    val name: String,
    val description: String? = null,
    val movementTypes: Set<MovementType>
)
