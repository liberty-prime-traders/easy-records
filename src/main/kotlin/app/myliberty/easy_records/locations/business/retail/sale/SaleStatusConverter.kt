package app.myliberty.easy_records.locations.business.retail.sale

import jakarta.persistence.Converter
import app.myliberty.easy_records.locations.business.retail.sale.api.SaleStatus
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class SaleStatusConverter : EnumConverter<SaleStatus>(SaleStatus::class.java)
