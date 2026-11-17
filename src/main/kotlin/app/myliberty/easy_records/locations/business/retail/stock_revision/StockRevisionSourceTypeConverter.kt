package app.myliberty.easy_records.locations.business.retail.stock_revision

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class StockRevisionSourceTypeConverter : EnumConverter<StockRevisionSourceType>(StockRevisionSourceType::class.java)
