package app.myliberty.easy_records.locations.business.retail.stock_adjustment

import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentCreateDto
import app.myliberty.easy_records.locations.business.retail.stock_adjustment.api.StockAdjustmentDirection
import app.myliberty.easy_records.util.business.StringUtils
import app.myliberty.easy_records.util.exceptions.LibertyGenericException

object StockAdjustmentValidator {

    fun guardCategoryRequest(stockAdjustmentCreateDto: StockAdjustmentCreateDto): StockAdjustmentDirection {
        val category = stockAdjustmentCreateDto.category
        val direction = resolveDirection(stockAdjustmentCreateDto)
        guardUnitCost(stockAdjustmentCreateDto, direction)
        if (StockAdjustmentCategoryRules.requiresReason(category)) {
            if (stockAdjustmentCreateDto.stockMovementReasonId == null) {
                throw LibertyGenericException("A reason is required for the Other category")
            }
        } else if (stockAdjustmentCreateDto.stockMovementReasonId != null) {
            throw LibertyGenericException("A reason is only accepted for the Other category")
        }
        if (StockAdjustmentCategoryRules.requiresNote(category) && StringUtils.getValueOrNull(stockAdjustmentCreateDto.note) == null) {
            throw LibertyGenericException("A note is required for this category")
        }
        return direction
    }

    private fun resolveDirection(stockAdjustmentCreateDto: StockAdjustmentCreateDto): StockAdjustmentDirection {
        val fixedDirection = StockAdjustmentCategoryRules.fixedDirection(stockAdjustmentCreateDto.category)
        val requestedDirection = stockAdjustmentCreateDto.direction
        if (fixedDirection != null) {
            if (requestedDirection != null && requestedDirection != fixedDirection) {
                throw LibertyGenericException("This category only allows a stock ${fixedDirection.name.lowercase()}")
            }
            return fixedDirection
        }
        return requestedDirection ?: throw LibertyGenericException("Direction is required for the Other category")
    }

    private fun guardUnitCost(stockAdjustmentCreateDto: StockAdjustmentCreateDto, direction: StockAdjustmentDirection) {
        if (StockAdjustmentCategoryRules.requiresUserSuppliedUnitCost(direction)) {
            stockAdjustmentCreateDto.requiredUnitCost()
        } else if (stockAdjustmentCreateDto.unitCost != null) {
            throw LibertyGenericException("Unit cost is taken from stock for a stock decrease")
        }
    }
}
