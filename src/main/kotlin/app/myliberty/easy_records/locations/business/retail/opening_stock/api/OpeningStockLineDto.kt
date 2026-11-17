package app.myliberty.easy_records.locations.business.opening_stock.api

import com.fasterxml.jackson.annotation.JsonProperty
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class OpeningStockLineDto(
    val locationProductId: UUID,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: PositiveUserProvidedQuantity,
    val unitId: UUID,
    val unitCost: PositiveMoney,
    /** The unit [unitCost] is priced per; defaults to [unitId]. */
    val costUnitId: UUID? = null
) {
    val resolvedCostUnitId: UUID = costUnitId ?: unitId
}
