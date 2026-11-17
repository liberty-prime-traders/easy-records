package app.myliberty.easy_records.organizations.business.accounting_config.api

import java.time.DayOfWeek
import java.util.UUID

data class OrgAccountingConfigResponseDto(
    val id: UUID,
    val fiscalYearEndMonth: Int,
    val fiscalPeriodCycle: FiscalPeriodCycle,
    val periodWeekStartDay: DayOfWeek,
    val periodPrepDays: Int
)
