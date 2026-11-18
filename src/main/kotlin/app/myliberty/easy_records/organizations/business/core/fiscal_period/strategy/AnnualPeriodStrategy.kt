package app.myliberty.easy_records.organizations.business.core.fiscal_period.strategy

import app.myliberty.easy_records.organizations.business.core.accounting_config.api.OrgAccountingConfigDto
import app.myliberty.easy_records.organizations.business.core.fiscal_period.FiscalPeriodUtils
import java.time.LocalDate

class AnnualPeriodStrategy : PeriodCycleStrategy {

    override fun nextPeriod(lastPeriodEnd: LocalDate, config: OrgAccountingConfigDto): PeriodRange {
        val start = lastPeriodEnd.plusDays(1)
        return PeriodRange(start, FiscalPeriodUtils.yearEnd(start, config.fiscalYearEndMonth))
    }

    override fun nextCleanStart(from: LocalDate, config: OrgAccountingConfigDto): LocalDate {
        val yearEnd = FiscalPeriodUtils.yearEnd(from, config.fiscalYearEndMonth)
        val yearStart = FiscalPeriodUtils.yearStart(yearEnd)
        return if (from == yearStart) from else yearEnd.plusDays(1)
    }
}
