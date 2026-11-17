package app.myliberty.easy_records.locations.business.retail.stock_adjustment.api

import java.math.BigDecimal

data class StockAdjustmentCostChange(
    val adjustmentReferenceNumber: String,
    val oldCost: BigDecimal,
    val newCost: BigDecimal,
)
