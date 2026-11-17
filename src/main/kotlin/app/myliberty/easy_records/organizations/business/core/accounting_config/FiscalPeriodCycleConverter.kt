package app.myliberty.easy_records.organizations.business.core.accounting_config

import jakarta.persistence.Converter
import app.myliberty.easy_records.organizations.business.core.accounting_config.api.FiscalPeriodCycle
import app.myliberty.easy_records.util.enums.EnumConverter

@Converter(autoApply = true)
class FiscalPeriodCycleConverter : EnumConverter<FiscalPeriodCycle>(FiscalPeriodCycle::class.java)
