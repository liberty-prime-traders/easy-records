package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import com.fasterxml.jackson.annotation.JsonProperty
import app.myliberty.easy_records.locations.business.retail.stock.api.StockAdjustmentCategory
import app.myliberty.easy_records.util.business.PositiveMoney
import app.myliberty.easy_records.util.exceptions.RtsGenericException
import app.myliberty.easy_records.util.quantity.PositiveUserProvidedQuantity
import java.util.UUID

data class StockAdjustmentCreateDto(
    val locationProductId: UUID,
    val category: StockAdjustmentCategory,
    val direction: StockAdjustmentDirection? = null,
    @param:JsonProperty("quantity")
    val userProvidedQuantity: PositiveUserProvidedQuantity,
    val unitId: UUID,
    val stockMovementReasonId: UUID? = null,
    val note: String? = null,
    val unitCost: PositiveMoney? = null
) {

    fun requiredUnitCost(): PositiveMoney = unitCost ?: throw RtsGenericException("Unit cost is required for a stock increase")
}
