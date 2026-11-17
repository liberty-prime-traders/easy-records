package app.myliberty.easy_records.locations.business.stock_adjustment.api

import app.myliberty.easy_records.organizations.business.stock_movement_reason.api.MovementType
import app.myliberty.easy_records.util.enums.HasCode

enum class StockAdjustmentDirection(override val code: String, val movementType: MovementType) : HasCode {
    INCREASE("INC", MovementType.STOCK_ADJUSTMENT_INCREASE),
    DECREASE("DEC", MovementType.STOCK_ADJUSTMENT_DECREASE)
}
