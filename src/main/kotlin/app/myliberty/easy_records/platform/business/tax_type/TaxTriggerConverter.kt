package app.myliberty.easy_records.platform.business.tax_type

import jakarta.persistence.Converter
import app.myliberty.easy_records.platform.business.tax_type.api.TaxTrigger
import app.myliberty.easy_records.util.enums.EnumSetConverter

@Converter(autoApply = true)
class TaxTriggerConverter : EnumSetConverter<TaxTrigger>(TaxTrigger::class.java)
