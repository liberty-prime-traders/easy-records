package app.myliberty.easy_records.locations.business.tax_entry

import jakarta.persistence.Converter
import app.myliberty.easy_records.locations.business.tax_entry.api.TaxDirection
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class TaxDirectionConverter : EnumConverter<TaxDirection>(TaxDirection::class.java)
