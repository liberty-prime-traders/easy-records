package app.myliberty.easy_records.locations.business.retail.stock.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class StockEntryStatusConverter : EnumConverter<StockEntryStatus>(StockEntryStatus::class.java)
