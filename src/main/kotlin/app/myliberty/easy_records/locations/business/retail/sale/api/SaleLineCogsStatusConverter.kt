package app.myliberty.easy_records.locations.business.sale.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class SaleLineCogsStatusConverter : EnumConverter<SaleLineCogsStatus>(SaleLineCogsStatus::class.java)
