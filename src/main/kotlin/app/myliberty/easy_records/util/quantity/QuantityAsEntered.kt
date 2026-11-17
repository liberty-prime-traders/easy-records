package app.myliberty.easy_records.util.quantity

import java.util.UUID

data class QuantityAsEntered(
    val unitId: UUID,
    val storageUnitsPerUserProvidedUnit: Long,
    val userProvidedQuantityText: String,
)
