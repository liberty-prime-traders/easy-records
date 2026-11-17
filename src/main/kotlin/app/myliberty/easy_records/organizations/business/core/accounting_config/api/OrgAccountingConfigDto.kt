package app.myliberty.easy_records.organizations.business.core.accounting_config.api

import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.util.UUID

data class OrgAccountingConfigDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val fiscalYearEndMonth: Int,
    val fiscalPeriodCycle: FiscalPeriodCycle,
    val periodWeekStartDay: DayOfWeek,
    val periodPrepDays: Int
)
