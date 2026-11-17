package app.myliberty.easy_records.platform.business.tax_type.api

import jakarta.persistence.Converter
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class TaxCalculationMethodConverter : EnumConverter<TaxCalculationMethod>(TaxCalculationMethod::class.java)
