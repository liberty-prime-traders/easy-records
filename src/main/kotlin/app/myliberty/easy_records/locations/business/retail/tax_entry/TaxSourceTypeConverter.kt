package app.myliberty.easy_records.locations.business.tax_entry

import jakarta.persistence.Converter
import app.myliberty.easy_records.locations.business.tax_entry.api.TaxSourceType
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class TaxSourceTypeConverter : EnumConverter<TaxSourceType>(TaxSourceType::class.java)
