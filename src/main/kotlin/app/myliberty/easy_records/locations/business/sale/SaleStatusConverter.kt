package app.myliberty.easy_records.locations.business.sale

import jakarta.persistence.Converter
import app.myliberty.easy_records.locations.business.sale.api.SaleStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class SaleStatusConverter : EnumConverter<SaleStatus>(SaleStatus::class.java)
