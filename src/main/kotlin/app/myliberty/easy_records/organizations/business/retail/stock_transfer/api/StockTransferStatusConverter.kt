package app.myliberty.easy_records.organizations.business.stock_transfer.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class StockTransferStatusConverter : EnumConverter<StockTransferStatus>(StockTransferStatus::class.java)
