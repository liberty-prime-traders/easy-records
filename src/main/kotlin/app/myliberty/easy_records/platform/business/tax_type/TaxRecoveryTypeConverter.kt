package app.myliberty.easy_records.platform.business.tax_type

import jakarta.persistence.Converter
import app.myliberty.easy_records.platform.business.tax_type.api.TaxRecoveryType
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class TaxRecoveryTypeConverter : EnumConverter<TaxRecoveryType>(TaxRecoveryType::class.java)
