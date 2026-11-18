package app.myliberty.easy_records.organizations.business.core.accounting_config.api

import java.time.DayOfWeek

data class OrgAccountingConfigInsertDto(
    val fiscalYearEndMonth: Int,
    val fiscalPeriodCycle: FiscalPeriodCycle,
    val periodWeekStartDay: DayOfWeek,
    val periodPrepDays: Int
)
