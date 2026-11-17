package app.myliberty.easy_records.locations.business.stock_adjustment.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class StockAdjustmentCostStatusConverter : EnumConverter<StockAdjustmentCostStatus>(StockAdjustmentCostStatus::class.java)
