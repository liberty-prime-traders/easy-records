package app.myliberty.easy_records.locations.business.retail.stock_transfer

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class StockTransferReceiptStatusConverter : EnumConverter<StockTransferReceiptStatus>(StockTransferReceiptStatus::class.java)
