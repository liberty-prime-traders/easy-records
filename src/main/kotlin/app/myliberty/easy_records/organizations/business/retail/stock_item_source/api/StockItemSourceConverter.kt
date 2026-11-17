package app.myliberty.easy_records.organizations.business.stock_item_source.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class StockItemSourceConverter : EnumConverter<StockItemSource>(StockItemSource::class.java)
